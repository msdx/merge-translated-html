package com.githang.java;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Geek_Soledad (msdx.android@qq.com)
 */
public class MergeHtml {
    private static final Pattern CONTENT_PATTERN = Pattern.compile("(\\d+\\.)+(.*+)");
    private static Set<String> MERGE_NODE_NAME = new HashSet<String>();

    static {
        MERGE_NODE_NAME.add("html");
        MERGE_NODE_NAME.add("div");
        MERGE_NODE_NAME.add("head");
        MERGE_NODE_NAME.add("body");
        MERGE_NODE_NAME.add("table");
        MERGE_NODE_NAME.add("thead");
        MERGE_NODE_NAME.add("tbody");
        MERGE_NODE_NAME.add("tr");
        MERGE_NODE_NAME.add("title");
        MERGE_NODE_NAME.add("a");
        MERGE_NODE_NAME.add("dl");
        MERGE_NODE_NAME.add("dt");
        MERGE_NODE_NAME.add("dd");
        MERGE_NODE_NAME.add("ul");
    }

    public static void main(String[] args) throws IOException {
        File workDir;
        if (args.length < 1) {
//            workDir = new File("./resources/");
            System.out.println("java -jar merge-translated-html.jar [path]");
            return;
        } else {
            workDir = new File(args[0]);
        }
        File sourcesFolder = new File(workDir, "source");
        File targetFolder = new File(workDir, "target");

        if (!sourcesFolder.exists() || !targetFolder.exists()) {
            System.out.println(sourcesFolder.getPath() + " or " + targetFolder.getPath() + " is not exist.");
            return;
        }

        File outputFolder = new File(workDir, "output");
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }
        File[] targets = targetFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".html");
            }
        });
        for (File target : targets) {
            System.out.println("Start merge: " + target.getName());
            File source = new File(sourcesFolder, target.getName());
            Document result = mergeFileContent(source, target);
            System.out.println(result.html());
            File output = new File(outputFolder, target.getName());
            FileUtils.writeStringToFile(output, result.html(), "UTF-8");
        }
    }

    private static Document mergeFileContent(File source, File target) throws IOException {
        Document sourceRoot = Jsoup.parse(source, "UTF-8");
//        System.out.println(sourceRoot.toString());

        Document targetRoot = Jsoup.parse(target, "UTF-8");
//        System.out.println(targetRoot.toString());

        merge(sourceRoot.children(), targetRoot.children());

        return targetRoot;
    }

    private static void merge(Elements source, Elements target) {
        int size = source.size();
        for (int i = 0; i < size; i++) {
            merge(source.get(i), target.get(i));
        }
    }

    private static void merge(Element source, Element target) {
        String nodeName = target.nodeName();
        if (needMergeChild(nodeName) && target.children().size() > 0) {
            merge(source.children(), target.children());
        } else {
            if ("p".equalsIgnoreCase(nodeName)) {
                if ("title".equals(target.attr("class"))) {
                    mergeText(source.select("b").get(0), target.select("b").get(0));
                } else {
                    mergeParagraph(source, target);
                }
            } else if ("td".equalsIgnoreCase(nodeName)) {
                mergeParagraph(source, target);
            } else if (isHeading(nodeName)) {
                mergeHeading(source, target, nodeName);
            } else if ("title".equalsIgnoreCase(nodeName)) {
                mergeText(source, target);
            } else if ("dt".equalsIgnoreCase(nodeName)) {
                mergeParagraph(source, target);
            } else if ("li".equalsIgnoreCase(nodeName)) {
                mergeParagraph(source, target);
            } else if ("span".equalsIgnoreCase(nodeName)) {
                String attr = target.attr("class");
                if ("chapter".equalsIgnoreCase(attr) || "section".equalsIgnoreCase(attr)) {
                    mergeContents(source, target);
                } else {
                    mergeParagraph(source, target);
                }
            }
        }
    }

    private static void mergeHeading(Element source, Element target, String nodeName) {
        int level = Integer.valueOf(nodeName.substring(1, 2));
        Element newElem = target.parent().appendElement("h" + (level + 2));
        Iterator<Attribute> iterator = source.attributes().iterator();
        while (iterator.hasNext()) {
            Attribute attr = iterator.next();
            newElem.attr(attr.getKey(), attr.getValue());
        }
        newElem.append(source.html());
    }

    /**
     * 合并目录
     */
    private static void mergeContents(Element source, Element target) {
        final String sourceText = source.text();
        if (!target.html().equalsIgnoreCase(source.html())) {
            Matcher matcher = CONTENT_PATTERN.matcher(sourceText);
            if (matcher.find()) {
                target.child(0).appendText(" -" + matcher.group(2));
            }
        }
    }

    private static void mergeParagraph(Element source, Element target) {
        if (!target.html().equalsIgnoreCase(source.html())) {
            final String targetChildName = target.childNode(0).nodeName();
            final String sourceChildName = source.childNode(0).nodeName();

            if ("img".equalsIgnoreCase(targetChildName) && "img".equalsIgnoreCase(sourceChildName)) {
                mergeImage(target.child(0), source.child(0));
                return;
            }

            if (!"p".equals(targetChildName) || !"p".equals(sourceChildName)) {
                target.append("<br/>");
            }
            target.append(source.html());
        }
    }

    private static void mergeText(Element source, Element target) {
        if (target.text().trim().length() > 0) {
            target.append(" - ").append(source.html());
        }
    }

    private static void mergeImage(Element source, Element target) {
        final String targetAlt = target.attr("alt");
        final String sourceAlt = source.attr("alt");
        if (!targetAlt.equalsIgnoreCase(sourceAlt)) {
            target.attr("alt", targetAlt + " - " + sourceAlt);
        }
    }

    private static boolean isHeading(String name) {
        return name.matches("h\\d");
    }

    private static boolean needMergeChild(String name) {
        return MERGE_NODE_NAME.contains(name);
    }

}

package org.b3log.solo.util;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.b3log.latke.Latkes;
import org.b3log.latke.ioc.BeanManager;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.util.Callstacks;
import org.b3log.latke.util.Stopwatchs;
import org.b3log.solo.model.Option;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.jsoup.select.NodeVisitor;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

public final class Markdowns {

    private static final Logger LOGGER = LogManager.getLogger(Markdowns.class);

    private static final Map<String, JSONObject> MD_CACHE = new ConcurrentHashMap<>();

    private static final int MD_TIMEOUT = 10000;

    private static final DataHolder OPTIONS = new MutableDataSet()
            .set(com.vladsch.flexmark.parser.Parser.EXTENSIONS, Arrays.asList(
                    TablesExtension.create(),
                    TaskListExtension.create(),
                    StrikethroughExtension.create(),
                    AutolinkExtension.create()))
            .set(HtmlRenderer.SOFT_BREAK, "<br />\n");

    private static final com.vladsch.flexmark.parser.Parser PARSER =
            com.vladsch.flexmark.parser.Parser.builder(OPTIONS).build();

    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    public static String LUTE_ENGINE_URL = "http://localhost:8249";

    public static boolean LUTE_AVAILABLE;

    public static boolean SHOW_CODE_BLOCK_LN = false;
    public static boolean FOOTNOTES = false;
    public static boolean SHOW_TOC = false;
    public static boolean AUTO_SPACE = false;
    public static boolean FIX_TERM_TYPO = false;
    public static boolean CHINESE_PUNCT = false;
    public static boolean IMADAOM = false;
    public static boolean PARAGRAPH_BEGINNING_SPACE = false;
    public static boolean SPEECH = false;

    public static void loadMarkdownOption(final JSONObject preference) {
        final String showCodeBlockLnVal = preference.optString(org.b3log.solo.model.Option.ID_C_SHOW_CODE_BLOCK_LN);
        Markdowns.SHOW_CODE_BLOCK_LN = "true".equalsIgnoreCase(showCodeBlockLnVal);
        final String footnotesVal = preference.optString(org.b3log.solo.model.Option.ID_C_FOOTNOTES);
        Markdowns.FOOTNOTES = "true".equalsIgnoreCase(footnotesVal);
        final String showToCVal = preference.optString(org.b3log.solo.model.Option.ID_C_SHOW_TOC);
        Markdowns.SHOW_TOC = "true".equalsIgnoreCase(showToCVal);
        final String autoSpaceVal = preference.optString(org.b3log.solo.model.Option.ID_C_AUTO_SPACE);
        Markdowns.AUTO_SPACE = "true".equalsIgnoreCase(autoSpaceVal);
        final String fixTermTypoVal = preference.optString(org.b3log.solo.model.Option.ID_C_FIX_TERM_TYPO);
        Markdowns.FIX_TERM_TYPO = "true".equalsIgnoreCase(fixTermTypoVal);
        final String chinesePunctVal = preference.optString(org.b3log.solo.model.Option.ID_C_CHINESE_PUNCT);
        Markdowns.CHINESE_PUNCT = "true".equalsIgnoreCase(chinesePunctVal);
        final String IMADAOMVal = preference.optString(org.b3log.solo.model.Option.ID_C_IMADAOM);
        Markdowns.IMADAOM = "true".equalsIgnoreCase(IMADAOMVal);
        final String paragraphBeginningSpaceVal = preference.optString(org.b3log.solo.model.Option.ID_C_PARAGRAPH_BEGINNING_SPACE);
        Markdowns.PARAGRAPH_BEGINNING_SPACE = "true".equalsIgnoreCase(paragraphBeginningSpaceVal);
        final String speechVal = preference.optString(Option.ID_C_SPEECH);
        Markdowns.SPEECH = "true".equalsIgnoreCase(speechVal);
    }

    public static void clearCache() {
        MD_CACHE.clear();
    }

    public static String clean(final String html) {
        final Safelist whitelist = Safelist.relaxed();
        whitelist.addAttributes("pre", "class")
                .addAttributes("div", "class", "data-code")
                .addAttributes("span", "class")
                .addAttributes("code", "class");
        final Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);
        return Jsoup.clean(html, Latkes.getServePath(), whitelist, outputSettings);
    }

    public static String toHTML(final String markdownText) {
        if (StringUtils.isBlank(markdownText)) {
            return "";
        }

        final String cachedHTML = getHTML(markdownText);
        if (cachedHTML != null) {
            return cachedHTML;
        }

        String html = processMarkdown(markdownText);

        // Perform additional processing on the generated HTML
        html = processHTML(html);

        // Cache the result
        putHTML(markdownText, html);
        return html;
    }

    private static String processMarkdown(String markdownText) {
        String html;
        if (LUTE_AVAILABLE) {
            try {
                html = toHtmlByLute(markdownText);
            } catch (final Exception e) {
                LOGGER.log(Level.WARN, "Failed to use Lute [" + LUTE_ENGINE_URL + "] for markdown [md=" + StringUtils.substring(markdownText, 0, 256) + "]: " + e.getMessage());
                html = toHtmlByFlexmark(markdownText);
            }
        } else {
            html = toHtmlByFlexmark(markdownText);
        }
        return html;
    }

    private static String processHTML(String html) {
        final Document doc = Jsoup.parseBodyFragment(html);
        doc.select("a").forEach(a -> {
            final String src = a.attr("href");
            if (!StringUtils.startsWithIgnoreCase(src, Latkes.getServePath()) && !StringUtils.startsWithIgnoreCase(src, "#")) {
                a.attr("target", "_blank");
            }
            a.removeAttr("id");
        });

        final List<Node> toRemove = new ArrayList<>();
        doc.traverse(new NodeVisitor() {
            @Override
            public void head(final org.jsoup.nodes.Node node, int depth) {
                if (node instanceof org.jsoup.nodes.TextNode) {

                }
            }

            @Override
            public void tail(org.jsoup.nodes.Node node, int depth) {
            }
        });

        toRemove.forEach(Node::remove);

        doc.outputSettings().prettyPrint(false);
        Images.qiniuImgProcessing(doc);

        String ret = doc.body().html();
        ret = StringUtils.trim(ret);
        return ret;
    }

    private static String toHtmlByLute(final String markdownText) throws Exception {
        final URL url = new URL(LUTE_ENGINE_URL);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("X-CodeSyntaxHighlightLineNum", String.valueOf(Markdowns.SHOW_CODE_BLOCK_LN));
        conn.setRequestProperty("X-Footnotes", String.valueOf(Markdowns.FOOTNOTES));
        conn.setRequestProperty("X-ToC", String.valueOf(Markdowns.SHOW_TOC));
        conn.setRequestProperty("X-AutoSpace", String.valueOf(Markdowns.AUTO_SPACE));
        conn.setRequestProperty("X-FixTermTypo", String.valueOf(Markdowns.FIX_TERM_TYPO));
        conn.setRequestProperty("X-ChinesePunct", String.valueOf(Markdowns.CHINESE_PUNCT));
        conn.setRequestProperty("X-IMADAOM", String.valueOf(Markdowns.IMADAOM));
        conn.setRequestProperty("X-ParagraphBeginningSpace", String.valueOf(Markdowns.PARAGRAPH_BEGINNING_SPACE));
        conn.setRequestProperty("X-HeadingID", "true");
        conn.setConnectTimeout(100);
        conn.setReadTimeout(3000);
        conn.setDoOutput(true);

        try (final OutputStream outputStream = conn.getOutputStream()) {
            IOUtils.write(markdownText, outputStream, "UTF-8");
        }

        String ret;
        try (final InputStream inputStream = conn.getInputStream()) {
            ret = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        }

        conn.disconnect();
        return ret;
    }

    private static String toHtmlByFlexmark(final String markdownText) {
        com.vladsch.flexmark.util.ast.Node document = PARSER.parse(markdownText);
        return RENDERER.render(document);
    }

    private static String getHTML(final String markdownText) {
        final String hash = DigestUtils.md5Hex(markdownText);
        final JSONObject value = MD_CACHE.get(hash);
        if (null == value) {
            return null;
        }
        return value.optString("data");
    }

    private static void putHTML(final String markdownText, final String html) {
        final String hash = DigestUtils.md5Hex(markdownText);
        final JSONObject value = new JSONObject();
        value.put("data", html);
        MD_CACHE.put(hash, value);
    }

    private Markdowns() {
    }
}

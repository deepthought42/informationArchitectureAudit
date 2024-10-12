package utils;

import static org.junit.Assert.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import com.looksee.audit.informationArchitecture.services.BrowserService;

public class XPathGeneratorTest {

    @Test
    public void testGetXPath_SingleElement() {
        String html = "<div><span>First</span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").first();

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[1]", xpath);
    }

    @Test
    public void testGetXPath_MultipleSiblings() {
        String html = "<div><span>First</span><span>Second</span><span>Third</span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(1); // Select the second <span> element

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[2]", xpath);
    }

    @Test
    public void testGetXPath_NestedElements() {
        String html = "<div><span>First</span><div><span>Nested</span></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(1); // Select the second <span> element (Nested)

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[1]/span[1]", xpath);
    }

    @Test
    public void testGetXPath_MultipleNestedElements() {
        String html = "<div><span>First</span><div><span>Nested</span><span>Deep Nested</span></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(2); // Select the third <span> element (Deep Nested)

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[1]/span[2]", xpath);
    }

    @Test
    public void testGetXPath_MultipleSameTagAncestors() {
        String html = "<div><div><span>First</span></div><div><span>Second</span></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").get(1); // Select the second <span> element

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[2]/span[1]", xpath);
    }

    @Test
    public void testGetXPath_DeeplyNestedElement() {
        String html = "<div><div><div><div><span>Deep</span></div></div></div></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").first();

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/div[1]/div[1]/div[1]/span[1]", xpath);
    }

    @Test
    public void testGetXPath_ElementWithSiblingsAndNestedChildren() {
        String html = "<div><span>First</span><span><div><p>Nested Paragraph</p></div></span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("p").first(); // Select the <p> element inside the second <span>

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[2]/div[1]/p[1]", xpath);
    }

    @Test
    public void testGetXPath_ElementWithoutSiblings() {
        String html = "<div><span>Only Child</span></div>";
        Document doc = Jsoup.parse(html);
        Element element = doc.select("span").first();

        String xpath = BrowserService.getXPath(element);
        assertEquals("//body/div[1]/span[1]", xpath);
    }
}
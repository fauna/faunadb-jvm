package com.faunadb.common.http;

//import com.faunadb.client.FaunaClient;
import com.faunadb.common.Connection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class CheckLatestVersion
{
    private static boolean alreadyChecked = false;

    public static void setAlreadyCheck() {
        alreadyChecked = true;
    }

    private static String urlString = "https://repo.maven.apache.org/maven2/com/faunadb/faunadb-common/maven-metadata.xml";
    private static boolean javaDriver;

    public static boolean getAlreadyChecked() {
        return alreadyChecked;
    }
    public static void checkLatestVersion() {
        try {
            if (!alreadyChecked) {
                getVersion();
            } else {
                return;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }
        setAlreadyCheck();
    }
    private static void getVersion() throws IOException, ParserConfigurationException, SAXException {
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(conn.getInputStream());
        var repositoryVersion = document.getDocumentElement().getElementsByTagName("latest").item(0).getFirstChild().getNodeValue();
        var driverVersion = Connection.class.getPackage().getSpecificationVersion();
        if (!repositoryVersion.equals(driverVersion))
        {
            System.out.println("\n");
            System.out.println("=".repeat(80));
            System.out.println("New fauna version available "+repositoryVersion +" -> "+ driverVersion);
            System.out.println("Changelog: https://github.com/fauna/faunadb-jvm/blob/master/CHANGELOG.txt");
            System.out.println("=".repeat(80));
            System.out.println("\n");
        }
    }

}

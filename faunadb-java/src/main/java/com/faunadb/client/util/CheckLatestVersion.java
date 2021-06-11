package com.faunadb.client.util;

import com.faunadb.client.FaunaClient;
import com.faunadb.common.Connection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class CheckLatestVersion
{
    public static void getVersion() throws IOException, ParserConfigurationException, SAXException {
        String urlString = "https://repo.maven.apache.org/maven2/com/faunadb/faunadb-java/maven-metadata.xml";
        URL url = new URL(urlString);
        URLConnection conn = url.openConnection();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(conn.getInputStream());
        var repositoryVersion = document.getDocumentElement().getElementsByTagName("latest").item(0).getFirstChild().getNodeValue();

        FaunaClient rootClient = new FaunaClient(Connection.builder().build());
        var ver1 = rootClient.getClass().getPackage().getSpecificationVersion();
        var ver2 = rootClient.getClass().getPackage().getImplementationVersion();
        //Package[] packages = Package.getPackages();
    }

}

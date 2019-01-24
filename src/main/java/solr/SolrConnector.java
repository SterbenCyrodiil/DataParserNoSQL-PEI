package solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class SolrConnector {


    private SolrClient solrClient;

    public SolrConnector(){
        //instância-se o cliente. Atenção ao nome da coleção/core
        solrClient= new HttpSolrClient.Builder("http://localhost:8983/solr/xmlteste").build();
    }

    /**
     * @param xml o próprio do ficheiro criado, na API aparece como "body", mas não explica o parâmetro.
     */
    public void addDocument(String xml){

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        Document doc = null;
        try {
            builder = factory.newDocumentBuilder();
            doc = builder.parse(new File(xml));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Element root = doc.getDocumentElement();
        String content = root.toString();

        //O que aqui faz é pedir e receber o XML para o requesthandler
        DirectXmlRequest xmlRequest = new DirectXmlRequest("/update", content);
        ModifiableSolrParams params = new ModifiableSolrParams();
        //Aqui define os parâmetros que irá executar para o datahandler
        params.set("command", "full-import");
        params.set("clean", "false");
        xmlRequest.setParams(params);
        try {
            //Aqui, executa o request na collection/core pedida
            solrClient.request(xmlRequest);
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String simpleQuery(String item, String value){

        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put(item, value);
        MapSolrParams queryParams = new MapSolrParams(queryMap);

        QueryResponse response = new QueryResponse();
        SolrDocumentList documents;
        try {
            response = solrClient.query(queryParams);

        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        documents = response.getResults();
        return documents.toString();
    }

    public String ascendingQuery(String item, String value){

        final Map<String, String> queryMap = new HashMap<>();
        queryMap.put(item, value);
        queryMap.put("sort",item+" asc");
        MapSolrParams queryParams = new MapSolrParams(queryMap);

        QueryResponse response = new QueryResponse();
        SolrDocumentList documents;
        try {
            response = solrClient.query(queryParams);

        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        documents = response.getResults();
        return documents.toString();


    }

    private long numberDocumetns(SolrDocumentList doc){
        return doc.getNumFound();
    }

}

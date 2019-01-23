package solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.DirectXmlRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.io.IOException;
import java.nio.file.Files;

public class SolrConnector {


    private SolrClient solrClient;

    public SolrConnector(){
        //instância-se o cliente. Atenção ao nome da coleção/core
        solrClient= new HttpSolrClient.Builder("http://localhost:8983/solr/collectionname").build();
    }

    /**
     * @param filesDir o diretório para o ficheiro criado
     * @param xml o próprio do ficheiro criado, na API aparece como "body", mas não explica o parâmetro.
     */
    public void addDocument(String filesDir, String xml){

        //O que aqui faz é pedir e receber o XML para o requesthandler
        DirectXmlRequest xmlRequest = new DirectXmlRequest(filesDir, xml);
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

}

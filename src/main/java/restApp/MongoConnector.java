package restApp;

import transformer.XSLTransformer;
import validator.XMLvalidator;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.*;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.eq;

public class MongoConnector {

    private MongoClient mongoClient;
    private static final String noSuchCollectionMsg = "Collection does not exist!";

    public MongoConnector() {
        mongoClient = new MongoClient(new MongoClientURI("mongodb://localhost:27017"));
    }

    /**
     * Construtor com a 'connection string' parametrizada.
     * NOTA: utilizar o default constructor para usar a string predefinida - "mongodb://localhost:27017"
     *
     * @param connection string com a 'connection string'
     */
    public MongoConnector(String connection) {
        mongoClient = new MongoClient(new MongoClientURI(connection));
    }

    /**
     * (Ainda em construção.) Tranformação dos docs encontrados pela query no mongoDB para XML
     */
    private void aggregateResultToXML(AggregateIterable<Document> results) {
        //algoritmo baseado nos exemplos disponíveis no repositório GIT da UC
        MongoCursor it = results.iterator();
        /*o resultado da pesquisa so vai devolver 1 documento, no entanto deixa-se ficar este ciclo while para a
        'flexibilidade' deste método. */
        while (it.hasNext()) {
            Document obj = (Document) it.next();
            //a data ja se encontra formatada em "ISODate", realizado pela propria pesquisa no mongoDB (ver query)

            JSONObject json = new JSONObject(obj.toJson());
            String xml = XML.toString(json, "root").replace("$", "");

            //invocação da classe responsável por aplicar o XSL (ter atenção às diretorias dos ficheiros!)
            String filesDir = "XMLgerados_XSDschemas_XSLTtemplate/";
            String xmlName = XSLTransformer.transform(xml, filesDir + "XSLTfiles/XSLTparaDefinicaoXML.xsl",
                    filesDir + "XMLgerados/", "XMLauditoria");

            try {
                //validação do XML criado pelo vocabulário relaltivo ao problema
                if (!XMLvalidator.validate(new File(filesDir + "XMLgerados/" + xmlName),
                        new File(filesDir + "SchemasDefinicaoModulos/SchemaAuditoriaLoja.xsd"))) {

                    BufferedWriter bw = new BufferedWriter(new FileWriter(
                            new File(filesDir + "XMLgerados/WARNINGS.txt")));
                    bw.append("WARNING:\nFicheiro de nome - " + xmlName + " - não validado pelo Schema - \n" +
                            filesDir + "SchemasDefinicaoModulos/SchemaAuditoriaLoja.xsd -, resolver!\n" +
                            "(stacktrace imprimida na consola durante a ultima execução!)\n");
                    bw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Procura na base de dados a existência da Venda relativa a uma loja e uma determinada data.
     *
     * @param storeField ID da loja a ser encontrada
     * @param saleMonth  mês referente à data (integer)
     * @param saleYear   ano referente à data (integer)
     * @return
     */
    public String getSaleDetails(String storeField, String saleMonth, String saleYear) {
        /*
        Eu coloquei a database e a collection que tenho no meu localhost!
        Ter isso em atenção para não dar problemas!
        (talvez até pode ser melhor parametrizar isso no REST)
         */
        MongoDatabase database = mongoClient.getDatabase("bikeOnTrackDB");
        MongoCollection<Document> collection = database.getCollection("salesDetails");

        /* ALTERAÇÃO PARA A ENTREGA FINAL
        Na entrega final descobriu-se que era necessário juntar todas as ReceiptLines num só documento! (já que
        a query anterior o que fazia era devolver vários documentos que continham algumas informações repetidas
        sobre a venda(como o cliente, loja, currencyRateID, ...))
        O que se fez foi adicionar um stage $group no final que basicamente junta todas essas receipt lines num
        documento embutido, sendo devolvido no final um unico ficheiro com toda a informação da venda da loja na data
        especificada!*/
        String query
                //formatar a data para ISODate ...
                = "[{$addFields:{" +
                "OrderDate:{$dateFromString:{dateString:\"$OrderDate\"}}}}," +
                //adicionar o documento embutido com a data ...
                "{$addFields:{" +
                "\"Date.year\":{$year:\"$OrderDate\"}," +
                "\"Date.month\":{$month:\"$OrderDate\"}," +
                "\"Date.day\":{$dayOfMonth:\"$OrderDate\"}}}," +
                //procura a venda da loja na data especificada ...
                "{$match:{" +
                "Store:" + storeField + ",\"Date.month\":" + saleMonth + ",\"Date.year\":" + saleYear + "}}," +
                //elimina o documento embutido (utilizado so para facilitar a pesquisa) ...
                "{$project:{" +
                "\"Date\":0}}," +
                //organização em um só documento (as linhas de venda são colocadas num documento embutido "ReceiptLines)
                "{$group:{" +
                "_id:null," +
                "ReceiptID:{$first:\"$ReceiptID\"}," +
                "OrderDate:{$first:\"$OrderDate\"}," +
                "Customer:{$first:\"$Customer\"}," +
                "CurrencyRateID:{$first:\"$CurrencyRateID\"}," +
                "SubTotal:{$first:\"$SubTotal\"}," +
                "TaxAmt:{$first:\"$TaxAmt\"}," +
                "Store:{$first:\"$Store\"}," +
                "StoreName:{$first:\"$StoreName\"}," +
                "ReceiptLines:{" +
                "$push:{" +
                "ReceiptLineID:\"$ReceiptLineID\"," +
                "Quantity:\"$Quantity\"," +
                "ProductID:\"$ProductID\"," +
                "UnitPrice:\"$UnitPrice\"," +
                "LineTotal:\"$LineTotal\"}}}}," +
                //faz-se parse da data para string novamente para poder ser utilizada no XSLT
                "{$addFields:{" +
                "OrderDate:{$dateToString:{date:\"$OrderDate\"}}}}]";

        List<Document> queryStages = this.getAggregateStagesFromString(query);
        //aggregate
        AggregateIterable<Document> results = collection.aggregate(queryStages);
        //Criar o XML do resultado
        aggregateResultToXML(results);

        //Mapear o resultado para um array em JSON
        return StreamSupport.stream(results.spliterator(), false)
                .map(Document::toJson)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Verifica se a collection introduzida existe.
     *
     * @param databaseName   nome da database
     * @param collectionName nome da collection
     * @return true ou false consoante a existência da collection
     */
    public boolean isCollection(String databaseName, String collectionName) {
        MongoDatabase database = mongoClient.getDatabase(databaseName);

        MongoIterable<String> collectionNames = database.listCollectionNames();
        for (final String name : collectionNames) {
            if (name.equalsIgnoreCase(collectionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retorna o resultado da procura relativa ao campo com o valor introduzido.
     *
     * @param databaseName   nome da database
     * @param collectionName nome da collection
     * @param field          nome do campo
     * @param value          valor do campo
     * @return String em formato JSON com os resultados
     */
    public String getFieldData(String databaseName, String collectionName, String field, String value) {
        if (!this.isCollection(databaseName, collectionName)) {
            return noSuchCollectionMsg;
        }
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Bson filter = eq(field, value);

        //NOTA: Apenas apresenta os 10 primeiros resultados (limit(10))
        return StreamSupport.stream(collection.find(filter).limit(10).spliterator(), false)
                .map(Document::toJson)
                .collect(Collectors.joining(", ", "[", "]")).toString();
    }

    /**
     * Método que permite aplicar vários stages ao método aggregate
     *
     * @param databaseName   nome da database
     * @param collectionName nome da collection
     * @param query          A query deverá ser enviada com os parêntesis retos
     *                       (representando os vários stages aplicados ao método aggregate.
     *                       Por exemplo: "[{},{}]"
     * @return String em formato JSON com os resultados
     */
    public String aggregateDataByQueryString(String databaseName, String collectionName, String query) {
        if (!this.isCollection(databaseName, collectionName)) {
            return noSuchCollectionMsg;
        }
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        List<Document> myList = this.getAggregateStagesFromString(query);

        //Mapear o resultado para um array em JSON
        return StreamSupport.stream(collection.aggregate(myList).spliterator(), false)
                .map(Document::toJson)
                .collect(Collectors.joining(", ", "[", "]")).toString();
    }

    /**
     * Método responsável por interpretar a consulta aplicada ao método
     * aggregate de forma a aplicar vários stages
     *
     * @param query A query deverá ser enviada com os parêntesis retos
     *              (representando os vários stages aplicados ao método aggregate. Por
     *              exemplo: "[{},{}]"
     * @return Lista de documentos JSON com a composição de cada stage
     */
    private List<Document> getAggregateStagesFromString(String query) {
        List<Document> myList = new ArrayList<>();
        try {
            JSONArray jsonObj = new JSONArray(query);
            for (int i = 0; i < jsonObj.length(); i++) {
                myList.add(Document.parse(jsonObj.get(i).toString()));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return myList;
    }

}

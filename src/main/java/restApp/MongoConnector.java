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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
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
     * Procura na base de dados a existência da Venda relativa a uma loja e uma determinada data.
     *
     * @param storeField ID da loja a ser encontrada
     * @param saleMonth  mês referente à data (integer)
     * @param saleYear   ano referente à data (integer)
     * @return
     */
    public String getSaleDetails(String databaseName, String collectionName, String storeField, String saleMonth, String saleYear) {
        /*
        Tal como estava escrito neste coment anteriormente, a database e collection foram parametrizadas no REST
        (no entanto, estão definidos parametros predefinidos (repetivamente, "bikestore" e "salesdetails"
         */
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);

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
                "LineTotal:\"$LineTotal\"}}}}]";

        List<Document> queryStages = this.getAggregateStagesFromString(query);
        //aggregate
        AggregateIterable<Document> results = collection.aggregate(queryStages);
        //Criar o XML do resultado
        this.aggregateResultsToXML(results);

        //Mapear o resultado para um array em JSON
        return StreamSupport.stream(results.spliterator(), false)
                .map(Document::toJson)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Tranformação dos docs encontrados pela query no mongoDB para um documento XML!
     * (algoritmo baseado nos exemplos disponíveis no repositório GIT da UC)
     * <p>
     * DEVELOPMENT NOTE: A xml string, que vai ser utilizada na transformação do XML a ser gerado, vai conter a
     * informação dos 3 CSV necessários (SalesDetails, ProductDetails e CurrencyDetails), tal como a informação
     * relativa às pesquisas sobre a informação adicional a ser introduzida.
     * <p>
     * Isto é, estará tudo organizado em diferentes elementos complexos relativos a cada parte referida
     * anteriormente, descendentes de um elemento pai "/root".
     *
     * @param results instância iterável, devolvida pelo uso do método agregate do mongoDB, com os resultados.
     */
    private void aggregateResultsToXML(AggregateIterable<Document> results) {
        MongoCursor it = results.iterator();
        /*o resultado da pesquisa so vai devolver 1 documento, no entanto deixa-se ficar este ciclo while para a
        'flexibilidade' deste método. */
        while (it.hasNext()) {
            Document obj = (Document) it.next();
            //Formata a data ISODate para um formato "xs:dateTime"
            this.dateFormat(obj, "OrderDate");

            JSONObject json = new JSONObject(obj.toJson());
            StringBuilder xmlTotal = new StringBuilder().append("<root>");
            xmlTotal.append(XML.toString(json, "DadosVenda").replace("$", ""));
            xmlTotal.append(this.getXmlDadosProdutos(json.getJSONArray("ReceiptLines")));
            /*
            Quando a CurrencyRateID == "NULL" (String) significa que não há taxa de câmbio e a moeda utilizada
            é o USD (isto já está resolvido no XSLT, logo quando acontece simplesmente não adiciona os dados à xml String)
            */
            if (!(json.get("CurrencyRateID") instanceof String)) {
                xmlTotal.append(this.getXmlDadosMoeda(json.getInt("CurrencyRateID")));
            }
            //adicionar a parte da informacao adicional (metodo abaixo)
            xmlTotal.append("</root>");

            //invocação da classe responsável por aplicar o XSL (ter atenção às diretorias dos ficheiros!)
            String filesDir = "XMLgerados_XSDschemas_XSLTtemplate/";
            String xmlName = XSLTransformer.transform(xmlTotal.toString(), filesDir + "XSLTparaDefinicaoXML.xsl",
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
     * Substitui a Key do Documento, que contém como valor uma data (passado por parametro) com um formato de data
     * relativo ao tipo de dados XSD "xs:dateTime"
     *
     * @param docObj Documento com a key a ser substituida.
     * @param key    Key a substituir
     */
    private void dateFormat(Document docObj, String key) {
        SimpleDateFormat formattedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        formattedDate.setTimeZone(TimeZone.getTimeZone("UTC"));
        //substituição
        if (docObj.get(key) instanceof String) {
            try {
                DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                Date date = format.parse((String) docObj.get(key));
                docObj.replace(key, formattedDate.format(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }

        } else {
            docObj.replace(key, formattedDate.format(docObj.getDate(key)));
        }
    }

    /**
     * Procura na collection "productDetails" documentos relativos aos "ProductID" encontrados nos JSONObjects que
     * pertençam ao JSONArray passado em parametro.
     * <p>
     * De seguida, realiza o parse desses documentos para uma xml String com o formato:
     * <p>
     * "<DadosProdutos><DadosProduto>...</DadosProduto><DadosProduto>...</DadosProduto></DadosProdutos>"
     * <p>
     * São formatadas tambem as datas contidas nesses JSONObjects (através do uso do método "dataFormat")!
     *
     * @param array JSONArray com a informação a ser utilizada.
     * @return xml String com os resultados do parse JSON to XML
     */
    private String getXmlDadosProdutos(JSONArray array) {
        StringBuilder xmlDadosProdutos = new StringBuilder().append("<DadosProdutos>");
        Iterator<Object> it = array.iterator();
        while (it.hasNext()) {
            JSONObject jsonObj = (JSONObject) it.next();

            MongoCursor mongoIt = this.getIterableFieldData(
                    "bikeOnTrackDB", "productDetails", "ProductID", jsonObj.getInt("ProductID")).iterator();
            Document docObj = (Document) mongoIt.next();
            //Formata a data ISODate para um formato "xs:dateTime"
            if (!docObj.get("SellStartDate").equals("NULL")) {
                this.dateFormat(docObj, "SellStartDate");
            }
            if (!docObj.get("SellEndDate").equals("NULL")) {
                this.dateFormat(docObj, "SellEndDate");
            }
            JSONObject json = new JSONObject(docObj.toJson());
            xmlDadosProdutos.append(XML.toString(json, "DadosProduto").replace("$", ""));
        }
        xmlDadosProdutos.append("</DadosProdutos>");
        return xmlDadosProdutos.toString();
    }

    /**
     * Procura na collection "currencyDetails" o documento relativo ao "CurrencyRateID" que se encontra no documento
     * retornado da procura na collection "salesDetails".
     * <p>
     * De seguida, realiza o parse desse documento para uma xml String com o formato:
     * <p>
     * "<DadosMoeda>...</DadosMoeda>"
     * <p>
     * É também formatada a data que se encontra no documento retornado na procura (através do uso do método "dataFormat")!
     *
     * @param id id relativo à Key "CurrencyRateID"
     * @return xml String com os resultados do parse JSON to XML
     */
    private String getXmlDadosMoeda(int id) {
        MongoCursor mongoIt = getIterableFieldData(
                "bikeOnTrackDB", "currencyDetails", "CurrencyRateID", id).iterator();
        Document docObj = (Document) mongoIt.next();
        //Formata a data ISODate para um formato "xs:dateTime"
        this.dateFormat(docObj, "CurrencyRateDate");

        JSONObject json = new JSONObject(docObj.toJson());
        return XML.toString(json, "DadosMoeda").replace("$", "");
    }

    private String getXmlDadosPesquisasInformacaoAdicional(JSONArray array) {
        StringBuilder xmlDadosProdutos = new StringBuilder().append("<Informacao>");
        /*
        Resultados das queries de pesquisa (Informação Adicional)
         */
        xmlDadosProdutos.append("</Informacao>");
        return xmlDadosProdutos.toString();
    }

    /**
     * Retorna uma instância do 'Iterable' com os resultados da procura relativa ao campo com o valor introduzido.
     * (NOTA: "value" encontra-se como Object devido à possibilidade de incompatibilidade de dados na procura pelos
     * docs JSON [ver nota no método "getCollectionField"]
     *
     * @param databaseName   nome da database
     * @param collectionName nome da collection
     * @param field          nome do campo
     * @param value          valor do campo
     * @return 'FindIterable' com os resultados encontrados.
     */
    public FindIterable<Document> getIterableFieldData(String databaseName, String collectionName, String field, Object value) {
        if (!this.isCollection(databaseName, collectionName)) {
            return null;
        }
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Bson filter = eq(field, value);

        return collection.find(filter);
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
     * Retorna o resultado da procura relativa ao campo com o valor introduzido num formato JSONArray.
     * (NOTA: não funciona se "value" for um numero, pois só é passado como String e quando faz a procura nos docs
     * JSON existe uma incompatibilidade de dados!)
     *
     * @param databaseName   nome da database
     * @param collectionName nome da collection
     * @param field          nome do campo
     * @param value          valor do campo
     * @return String em formato JSON com os resultados
     */
    public String getCollectionField(String databaseName, String collectionName, String field, String value) {
        if (!this.isCollection(databaseName, collectionName)) {
            return noSuchCollectionMsg;
        }
        MongoDatabase database = mongoClient.getDatabase(databaseName);
        MongoCollection<Document> collection = database.getCollection(collectionName);
        Bson filter = eq(field, value);
        //NOTA: Apenas apresenta os 10 primeiros resultados (limit(10)) [retirado]
        return StreamSupport.stream(collection.find(filter).spliterator(), false)
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

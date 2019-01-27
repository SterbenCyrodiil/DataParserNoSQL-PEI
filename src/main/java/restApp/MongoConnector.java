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

import java.io.File;
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
     * Também realiza a transformação dessa informação (em JSON) para XML.
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
        String queryFormatacaoData =
                //formatar a data para ISODate ...
                "[{$addFields:{" +
                        "OrderDate:{$dateFromString:{dateString:\"$OrderDate\"}}}}," +
                        //adicionar o documento embutido com a data ...
                        "{$addFields:{" +
                        "\"Date.year\":{$year:\"$OrderDate\"}," +
                        "\"Date.month\":{$month:\"$OrderDate\"}," +
                        "\"Date.day\":{$dayOfMonth:\"$OrderDate\"}}},";
        String queryBuscaAuditoria =
                //procura a venda da loja na data especificada ...
                "{$match:{" +
                        "Store:" + storeField + ",\"Date.month\":" + saleMonth + ",\"Date.year\":" + saleYear + "}}," +
                        //elimina o documento embutido (utilizado so para facilitar a pesquisa) ...
                        "{$project:{\"Date\":0}},";
        String queryOrganizacaoReceiptLines =
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
        String query = queryFormatacaoData + queryBuscaAuditoria + queryOrganizacaoReceiptLines;

        List<Document> queryStages = this.getAggregateStagesFromString(query);

        /* REALIZAR A VERIFICAÇÃO DO LIST PRICE
         * -> Eu pensei em colocar a verificação dos ListPrices a 0 a fazer aqui na API automaticamente.
         * Descobri que é extremamente complicado. A query que temos em javascript para o mongoDB serve lindamente.
         * Fica esta mensagem aqui para escrever depois no relatório se for preciso.
         * this.verifyListPrice(database, collection);
         * */

        //aggregate, encontrar os resultados da pesquisa
        AggregateIterable<Document> results = collection.aggregate(queryStages);

        if (results.iterator().hasNext()) {
            //Criar o XML do resultado (quando existem resultados)
            this.aggregateResultsToXML(collection, results, queryFormatacaoData, queryBuscaAuditoria);
        }

        //Mapear o resultado para um array em JSON
        return StreamSupport.stream(results.spliterator(), false)
                .map(Document::toJson)
                .collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Tranformação dos docs JSON encontrados na collection pela query, no mongoDB, para um documento XML!
     * (algoritmo baseado nos exemplos disponíveis no repositório GIT da UC)
     * <p>
     * DEVELOPMENT NOTE: A xml string, que vai ser utilizada na transformação do XML a ser gerado, vai conter a
     * informação dos 3 CSV necessários (SalesDetails, ProductDetails e CurrencyDetails), tal como a informação
     * relativa às pesquisas sobre a informação adicional a ser introduzida.
     * <p>
     * Isto é, estará tudo organizado em diferentes elementos complexos relativos a cada parte referida
     * anteriormente, descendentes de um elemento pai "<root></root>".
     * <p>
     * Seguiu-se esta abordagem devido à facilidade que proporciona na formação da template no documento XSLT para a
     * transformação do XML a ser gerado.
     *
     * @param collection          coleção recebida pelo REST
     * @param results             instância iterável, devolvida pelo uso do método agregate do mongoDB, com os resultados.
     * @param queryFormatacaoData mongoDB query com a formatacao da data a ser utilizada na procura dos documentos
     * @param queryBuscaAuditoria mongoDB query para a procura da auditoria especificada na base de dados
     */
    public void aggregateResultsToXML(MongoCollection collection, AggregateIterable<Document> results,
                                      String queryFormatacaoData, String queryBuscaAuditoria) {
        MongoCursor it = results.iterator();

        /*o resultado da pesquisa so vai devolver 1 documento, no entanto deixa-se ficar este ciclo while para a
        'flexibilidade' deste método. */
        while (it.hasNext()) {
            Document obj = (Document) it.next();
            //Formata a data ISODate para um formato "xs:dateTime"
            this.dateFormat(obj, "OrderDate");

            JSONObject json = new JSONObject(obj.toJson());
            //String Builder que vai fazer append de todos os elementos que vão ser utilizados no XSLT
            StringBuilder xmlTotal = new StringBuilder().append("<root>");

            //adiciona à xml String os elementos XML com os dados relativos à Venda
            xmlTotal.append(XML.toString(json, "DadosVenda").replace("$", ""));

            //adiciona à xml String os elementos XML com os dados relativos aos ProductIDs que estejam nos dados da Venda
            xmlTotal.append(this.getXmlDadosProdutosRelativosReceiptLines(collection, json.getJSONArray("ReceiptLines")));

            /*
            adiciona à xml String os elementos XML com os dados relativos ao CurrencyRateID, e à respetiva moeda,
            que estejam nos dados da Venda

            Quando a CurrencyRateID == "NULL" (String) significa que não há taxa de câmbio e a moeda utilizada
            é o USD (isso é resolvido no XSLT, logo quando acontece simplesmente não adiciona os dados à xml String)
            */
            if (json.get("CurrencyRateID") instanceof String) {
            } else
                xmlTotal.append(this.getXmlDadosMoedaRelativosCurrencyRateID(collection, json.getInt("CurrencyRateID")));

            //adiciona à xml String os elementos XML com os dados relativos às informações adicionais sobre as lojas
            xmlTotal.append(this.getXmlDadosInformacaoAdicional(collection, queryFormatacaoData, queryBuscaAuditoria));

            xmlTotal.append("</root>");

            System.out.println("\n\nXML_TOTAL: " + xmlTotal.toString());
            //invocação da classe responsável por aplicar o XSL (ter atenção às diretorias dos ficheiros!)
            String filesDir = "XMLgerados_XSDschemas_XSLTtemplate/";
            String namespaceXmlName = XSLTransformer.transform(xmlTotal.toString(), filesDir + "XSLTdefinicaoNamespaceXML.xsl",
                    filesDir + "XMLgerados/", "XMLauditoria");
            String indexingXmlName = XSLTransformer.transform(xmlTotal.toString(), filesDir + "XSLTdefinicaoXMLIndexacaoApacheSolr.xsl",
                    filesDir + "XMLgerados/SolrIndexing/", "XMLauditoriaIndexing");
            System.out.println("\nFICHEIROS_CRIADOS:\n-> namespaceXML: " + namespaceXmlName + " | dir: " + filesDir + "XMLgerados/ \n" +
                    "\n-> indexingXML: " + indexingXmlName + " | dir: " + filesDir + "XMLgerados/SolrIndexing/ \n");
        }
    }

    /**
     * Substitui a Key do Documento, que contém como valor uma data (passado por parametro)
     * (aceita tanto "Date" como "String") com um formato de data relativo ao tipo de dados XSD "xs:dateTime"
     *
     * @param docObj Documento com a key a ser substituida.
     * @param key    Key a substituir
     */
    public void dateFormat(Document docObj, String key) {
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
     * "<DadosProdutos><DadosProduto>...</DadosProduto><DadosProduto>...</DadosProduto>...</DadosProdutos>"
     * <p>
     * São formatadas tambem as datas contidas nesses JSONObjects (através do uso do método "dataFormat")!
     *
     * @param collection coleção recebida pelo REST
     * @param array      JSONArray com a informação relativa às ReceiptLines (que contêm os produtos).
     * @return xml String com os resultados do parse JSON to XML relativo à informação sobre os produtos
     */
    public String getXmlDadosProdutosRelativosReceiptLines(MongoCollection collection, JSONArray array) {
        String collectionName;
        if (collection.getNamespace().getCollectionName().contains("salesDetails")) {
            collectionName = "productDetails";
        } else {
            collectionName = "productdetails";
        }
        StringBuilder xmlDadosProdutos = new StringBuilder().append("<DadosProdutos>");
        Iterator<Object> it = array.iterator();
        while (it.hasNext()) {
            JSONObject jsonObj = (JSONObject) it.next();
            MongoCursor mongoIt = this.getIterableFieldData(
                    collection.getNamespace().getDatabaseName(), collectionName, "ProductID", jsonObj.getInt("ProductID")).iterator();
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
     * @param collection coleção recebida pelo REST
     * @param id         id relativo à Key "CurrencyRateID"
     * @return xml String com os resultados do parse JSON to XML relativo à informação sobre a moeda
     */
    public String getXmlDadosMoedaRelativosCurrencyRateID(MongoCollection collection, int id) {
        String collectionName;
        if (collection.getNamespace().getCollectionName().contains("salesDetails")) {
            collectionName = "currencyDetails";
        } else {
            collectionName = "currencydetails";
        }
        MongoCursor mongoIt = getIterableFieldData(
                collection.getNamespace().getDatabaseName(), collectionName, "CurrencyRateID", id).iterator();
        Document docObj = (Document) mongoIt.next();
        //Formata a data ISODate para um formato "xs:dateTime"
        this.dateFormat(docObj, "CurrencyRateDate");

        JSONObject json = new JSONObject(docObj.toJson());
        return XML.toString(json, "DadosMoeda").replace("$", "");
    }

    /**
     * Utilizando as várias queries presentes no ficheiro de texto "Queries-Pesquisa-MongoDB.txt", que retornam as várias
     * informações adicionais para serem adicionadas ao XML a ser gerado, e através do método '.aggregate()' do mongoDB,
     * produz os documentos JSON com as informações pedidas no enunciado a serem adicionadas ao XML.
     * <p>
     * Resumidamente, o processo baseia-se na abordagem já seguida. Vão ser criados mais elementos, com através da
     * transformação dos documentos retornados em JSON para elementos XML, com a estrutura:
     * <p>
     * "<Informacao><nomeDaInformacao>...</nomeDaInformacao>...</Informacao>"
     * <p>
     * , e serão adicionados posteriormente à xml String a ser utilizada pelo XSLT para a produção automática dos
     * XML das auditorias.
     * <p>
     * (Comentários e explicação das queries utilizadas presente no documento de texto enviado em anexo com o nome
     * "Queries-Pesquisa-MongoDB.txt"
     *
     * @param collection          coleção recebida pelo REST
     * @param queryFormatacaoData mongoDB query com a formatacao da data a ser utilizada na procura dos documentos
     * @param queryBuscaAuditoria mongoDB query para a procura da auditoria especificada na base de dados
     * @return xml String com os resultados do parse JSON to XML relativo à todas as informações adicionais
     */
    public String getXmlDadosInformacaoAdicional(MongoCollection collection,
                                                 String queryFormatacaoData, String queryBuscaAuditoria) {
        StringBuilder xmlDadosInformacoes = new StringBuilder().append("<Informacao>");
        List<Document> queryStages;
        MongoCursor it;
        String query;
        String queryBuscaExercicio = "{$match:{" +
                //Igual à query de busca da auditoria só que sem a Store
                queryBuscaAuditoria.substring(queryBuscaAuditoria.indexOf("\"Date.month\":"));

        /* ==========================================================================================================
        QUERY - Total Produtos vendidos por cada Loja */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:{store:\"$Store\",product:\"$ProductID\"},Quantity:{$first:\"$Quantity\"}}}," +
                "{$group:{_id:\"$_id.store\",valor:{$sum:\"$Quantity\"}}},{$sort:{\"_id\":1}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlDadosInformacoes.append(XML.toString(obj, "TotalProdutosVendidosPorLoja").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Valor total das Vendas por cada Loja (subTotal + taxAmt) */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:{store:\"$Store\",sale:\"$ReceiptID\"},SubTotal:{$first:\"$SubTotal\"},TaxAmt:{$first:\"$TaxAmt\"}}}," +
                "{$project:{\"_id\":1,valorTotalVenda:{$add:[\"$SubTotal\",\"$TaxAmt\"]}}}," +
                "{$group:{_id:\"$_id.store\",valor:{$sum:\"$valorTotalVenda\"}}},{$sort:{\"_id\":1}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlDadosInformacoes.append(XML.toString(obj, "ValorTotalVendasPorLoja").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Valor médio do preço de venda (UnitPrice) dos produtos por cada loja */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:{store:\"$Store\",sale:\"$ReceiptID\"},mediaPrecoVendaProdutos:{$avg:\"$UnitPrice\"}}}," +
                "{$group:{_id:\"$_id.store\",valor:{$sum:\"$mediaPrecoVendaProdutos\"}}},{$sort:{\"_id\":1}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlDadosInformacoes.append(XML.toString(obj, "MediaPrecoVendaProdutosPorLoja").replace("$", ""));
        }

        //Venda
        xmlDadosInformacoes.append(getXmlInformacaoVenda(collection, queryFormatacaoData, queryBuscaAuditoria));

        //Exercicio
        xmlDadosInformacoes.append(getXmlInformacaoExercicio(collection, queryFormatacaoData, queryBuscaExercicio));

        xmlDadosInformacoes.append("</Informacao>");
        return xmlDadosInformacoes.toString();
    }

    /**
     * Expansão do método "getXmlDadosInformacaoAdicional", que gera os elementos especificos da informação adicional
     * relativa à venda.
     * <p>
     * (Comentários e explicação das queries utilizadas presente no documento de texto enviado em anexo com o nome
     * "Queries-Pesquisa-MongoDB.txt"
     *
     * @param collection          coleção recebida pelo REST
     * @param queryFormatacaoData mongoDB query com a formatacao da data a ser utilizada na procura dos documentos
     * @param queryBuscaAuditoria mongoDB query para a procura da auditoria especificada na base de dados
     * @return xml String com os resultados do parse JSON to XML relativo às informações adicionais sobre a venda
     */
    public String getXmlInformacaoVenda(MongoCollection collection,
                                        String queryFormatacaoData, String queryBuscaAuditoria) {
        StringBuilder xmlInformacoesVenda = new StringBuilder();
        List<Document> queryStages;
        MongoCursor it;
        String query;

        /* ==========================================================================================================
        QUERY - Total Produtos existentes na Venda */
        query = queryFormatacaoData + queryBuscaAuditoria +
                "{$group:{_id:null,valor:{$sum:\"$Quantity\"}}},{$project:{_id:0}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesVenda.append(XML.toString(obj, "TotalProdutosVenda").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Total Produtos Diferentes existentes na Venda */
        query = queryFormatacaoData + queryBuscaAuditoria +
                "{$group:{_id:\"$ProductID\"}},{$count:\"valor\"}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesVenda.append(XML.toString(obj, "TotalProdutosDiferentesVenda").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Valor médio do preço de venda (UnitPrice) dos produtos existentes na Venda */
        query = queryFormatacaoData + queryBuscaAuditoria +
                "{$group:{_id:null,valor:{$avg:\"$UnitPrice\"}}},{$project:{_id:0}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesVenda.append(XML.toString(obj, "MediaPrecoVendaProdutosVenda").replace("$", ""));
        }

        return xmlInformacoesVenda.toString();
    }

    /**
     * Expansão do método "getXmlDadosInformacaoAdicional", que gera os elementos especificos da informação adicional
     * relativa ao Exercicio.
     * <p>
     * (Comentários e explicação das queries utilizadas presente no documento de texto enviado em anexo com o nome
     * "Queries-Pesquisa-MongoDB.txt"
     *
     * @param collection          coleção recebida pelo REST
     * @param queryFormatacaoData mongoDB query com a formatacao da data a ser utilizada na procura dos documentos
     * @param queryBuscaExercicio mongoDB query para a procura do Exercicio relativo na base de dados
     * @return xml String com os resultados do parse JSON to XML relativo às informações adicionais sobre o exercicio
     */
    public String getXmlInformacaoExercicio(MongoCollection collection,
                                            String queryFormatacaoData, String queryBuscaExercicio) {
        StringBuilder xmlInformacoesExercicio = new StringBuilder();
        List<Document> queryStages;
        MongoCursor it;
        String query;

        /* ==========================================================================================================
        QUERY - Total Produtos existentes no Exericio */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:null,valor:{$sum:\"$Quantity\"}}},{$project:{_id:0}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesExercicio.append(XML.toString(obj, "TotalProdutosExercicio").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Total Produtos diferentes existentes no Exercicio */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:\"$ProductID\"}},{$count:\"valor\"}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesExercicio.append(XML.toString(obj, "TotalProdutosDiferentesExercicio").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Total Clientes diferentes existentes no Exercicio */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:\"$Customer\"}},{$count:\"valor\"}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesExercicio.append(XML.toString(obj, "TotalClientesDiferentesExercicio").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Valor vendido por cada cliente. Ordenar o valor vendido por ordem decrescente */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:\"$ReceiptID\",Customer:{$first:\"$Customer\"},SubTotal:{$first:\"$SubTotal\"},TaxAmt:{$first:\"$TaxAmt\"}}}," +
                "{$project:{\"_id\":1,\"Customer\":1,valorTotalVenda:{$add:[\"$SubTotal\",\"$TaxAmt\"]}}}," +
                "{$group:{_id:\"$Customer\", valor:{$sum:\"$valorTotalVenda\"}}},{$sort:{valorVendido:-1}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesExercicio.append(XML.toString(obj, "ValorVendidoClienteExercicio").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Total unidade vendidas por Produto. Resultados ordenados de forma descendente pelo número de unidades vendidas */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:\"$ProductID\", valor:{$sum:\"$Quantity\"}}},{$sort:{quantidadeVendida:-1}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesExercicio.append(XML.toString(obj, "TotalUnidadesVendidasProdutoExercicio").replace("$", ""));
        }
        /* ==========================================================================================================
        QUERY - Valor total da venda por cada moeda utilizada (valores a NULL já estão resolvidos no XSLT) */
        query = queryFormatacaoData + queryBuscaExercicio +
                "{$group:{_id:\"$ReceiptID\",CurrencyRateID:{$first:\"$CurrencyRateID\"},SubTotal:{$first:\"$SubTotal\"},TaxAmt:{$first:\"$TaxAmt\"}}}," +
                "{$project:{\"_id\":1,\"CurrencyRateID\":1,valorTotalVenda:{$add:[\"$SubTotal\",\"$TaxAmt\"]}}}," +
                "{$group:{_id:\"$CurrencyRateID\", valor:{$sum:\"$valorTotalVenda\"}}}]";
        queryStages = this.getAggregateStagesFromString(query);
        it = collection.aggregate(queryStages).iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            JSONObject obj = new JSONObject(doc.toJson());
            xmlInformacoesExercicio.append("<TotalVendaPorMoedaExercicio>");
            xmlInformacoesExercicio.append(XML.toString(obj, "CurrencyIDValorTotal").replace("$", ""));
            //Verificar se o CurrencyRateID é "NULL" e no contrário procurar pela Moeda correspondente
            if (obj.get("_id") instanceof String) {
            } else
                xmlInformacoesExercicio.append(this.getXmlDadosMoedaRelativosCurrencyRateID(collection, obj.getInt("_id")));
            xmlInformacoesExercicio.append("</TotalVendaPorMoedaExercicio>");
        }

        return xmlInformacoesExercicio.toString();
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
                .collect(Collectors.joining(", ", "[", "]"));
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
                .collect(Collectors.joining(", ", "[", "]"));
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
    public List<Document> getAggregateStagesFromString(String query) {
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

    /**
     * Valida um documento XML relativo ao Schema passado por parametro.
     * (No entanto, se "xmlName" for igual a "all", faz a validação para todos os documentos XML presentes na diretoria
     * especificada.)
     *
     * @param filesDir diretoria com o(s) ficheiro(os) XML
     * @param xmlName  nome do documento XML a verificar ("all" para validar tudo)
     * @param xsdPath  caminho para o XSD a ser utilizado para a validação
     * @return
     */
    public String validateXMLwithXSD(String filesDir, String xmlName, String xsdPath) {
        final String valido = "INFO: XML - %s - validado com sucesso!\n";
        final String invalido = "INFO: Não foi possível validar o XML - %s ! [stacktrace imprimida para a consola]\n";
        final String exc = "* ERROR: %s - File(s) not found! | Couldn't read file(s)! *\n" +
                "INFO: Valores predefinidos dos parametros:\n" +
                "-> xmlDirectory: \"XMLgerados_XSDschemas_XSLTtemplate/XMLgerados/\"\n" +
                "-> xsdFilePath: \"XMLgerados_XSDschemas_XSLTtemplate/SchemasDefinicaoModulos/SchemaAuditoriaLoja.xsd\"";

        if (xmlName.equalsIgnoreCase("all")) {
            StringBuilder str = new StringBuilder();
            File[] files = new File(filesDir).listFiles();
            try {
                for (File f : files) {
                    if (f.getName().contains(".xml")) {
                        if (XMLvalidator.validate(new File(filesDir + f.getName()), new File(xsdPath)))
                            str.append(String.format(valido, f.getName()));
                        else
                            str.append(String.format(invalido, f.getName()));
                    }
                }
                return str.toString();
            } catch (IOException e) {
                e.printStackTrace();
                return String.format(exc, filesDir);
            }

        } else {
            try {
                boolean bool;
                if (xmlName.contains(".xml")) {
                    bool = XMLvalidator.validate(new File(filesDir + xmlName), new File(xsdPath));
                } else {
                    bool = XMLvalidator.validate(new File(filesDir + xmlName + ".xml"), new File(xsdPath));
                }
                if (bool)
                    return String.format(valido, xmlName);
                else
                    return String.format(invalido, xmlName);
            } catch (IOException e) {
                e.printStackTrace();
                return String.format(exc, xmlName);
            }
        }
    }
}

package validator;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

/**
 * <h3>
 * ESTG - Escola Superior de Tecnologia e Gestão<br>
 * IPP - Instituto Politécnico do Porto <br>
 * LEI - Licenciatura em Engenharia Informática<br>
 * PEI - Processamento Estruturado de Informação<br>
 * 2017 / 2018 <br>
 * </h3>
 * <p>
 * <strong>Descrição:</strong>
 * Classe de exemplo que permite validar a sintaxe de um documento XML
 * (ligeiramente modificado - Grupo 4, Projeto AC 2018-2019 da UC)
 * <br>
 * </p>
 */
public class XMLvalidator {

    private final String xmlFile, xsdFile;
    private Document document;

    /**
     * @param xmlFile Documento XML a processar
     * @param xsdFile Documento XSD a processar
     */
    public XMLvalidator(String xmlFile, String xsdFile) {
        this.xmlFile = xmlFile;
        this.xsdFile = xsdFile;
    }

    /**
     * Método responsável por ler (parse) um documento XML
     *
     * @param xmlFile path para o documento xml
     * @return valor booleano sinalizando sucesso/insucesso da operação
     */
    public static Document readXML(String xmlFile) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document document = null;
        try {
            db = dbf.newDocumentBuilder();
            db.setErrorHandler(new XMLErrorHandler());
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            document = db.parse(xmlFile);
            document.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return document;
    }

    /**
     * Imprime o conteúdo de um  Documento XML para uma String.
     *
     * @param xml parsed XML document
     * @param stream stream onde será imprimido o conteúdo (String)
     */
    public static void printMenuXML(Document xml, StringWriter stream) {
        try {
            //criação do transformador de documentos
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer = tFactory.newTransformer();

            /**
             * Alteração das definições de output
             *
             * Neste caso 'INDENT' -> 'yes' serve para que as 'newlines' ('\n')
             * no output estejam corretas
             */
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            //conteúdo do ficheiro
            DOMSource source = new DOMSource(xml);
            //output do resultado
            StreamResult result = new StreamResult(stream);
            transformer.transform(source, result);

        } catch (NullPointerException | TransformerException exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Método responsável por validar um documento XML com o seu schema
     *
     * @param xml Documento XML a processar
     * @param xsd Documento XSD a processar
     * @return valor booleano sinalizando sucesso/insucesso da operação
     * @throws java.io.IOException exceção relativa à manipulação e existência dos ficheiros introduzidos
     */
    public static boolean validate(File xml, File xsd) throws IOException {
        Source schemaFile = new StreamSource(xsd), xmlFile = new StreamSource(xml);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            schemaFactory.setErrorHandler(new XSDErrorHandler());
            Schema schema = schemaFactory.newSchema(schemaFile);
            Validator validator = schema.newValidator();
            validator.setErrorHandler(new XSDErrorHandler());
            validator.validate(xmlFile);
        } catch (SAXException | IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Método que permite validar um documento XML previamento definido com o
     * seu schema também previamente definido. Sem a existência de um xsd
     * retorna "true"
     *
     * @return true se validado, false no contrário
     * @throws java.io.IOException exceção relativa à manipulação e existência dos ficheiros introduzidos
     */
    public boolean validate() throws IOException {
        return xsdFile == null || xsdFile.isEmpty() ? true : XMLvalidator.validate(new File(xmlFile), new File(xsdFile));
    }

    /**
     * Metodo que permite ler um ficheiro XML presente na instância deste objeto.
     *
     * @return true se leu, false no contrário (stacktrace na consola)
     */
    public boolean readXML() {
        return XMLvalidator.readXML(this.xmlFile) != null ? true : false;
    }
}

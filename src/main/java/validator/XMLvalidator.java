package validator;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
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
     * @return valor booleano sinalizando sucesso/insucesso da operação
     */
    public boolean readXML() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        try {
            db = dbf.newDocumentBuilder();
            db.setErrorHandler(new XMLErrorHandler());
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            document = db.parse(this.xmlFile);
            document.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
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

}

package transformer;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

/**
 * Classe de exemplo já disponibilizada: "ExemploJavaXSL", modificada para aceitar um documento XML como string ao
 * invés de um ficheiro.
 * (ligeiramente modificado)
 */
public class XSLTransformer {

    /**
     * Formata o XML (em string) recebido tendo em conta o XSLT fornecido.
     *
     * @param xmlDocument  documento xml em string (o próprio documento)
     * @param xslDocument  caminho para o documento XSL
     * @param xmlOutputDir diretoria para os XML gerados
     * @param xmlName      nome do XML a ser criado (sem a definição do ficheiro '.xml')
     * @return nome do ficheiro xml criado
     */
    public static String transform(String xmlDocument, String xslDocument, String xmlOutputDir, String xmlName) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        File stylesheet = new File(xslDocument);
        InputSource is = new InputSource(new StringReader(xmlDocument));
        DocumentBuilder builder;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(is);

            TransformerFactory tFactory = TransformerFactory.newInstance();
            StreamSource stylesource = new StreamSource(stylesheet);
            Transformer transformer = tFactory.newTransformer(stylesource);

            DOMSource source = new DOMSource(document);

            /*
            Procura se existem ficheiros na diretoria e numera o proximo ficheiro que vai ser criado, evitando diretamente
            conflitos com nomes. */
            File[] ficheiros = new File(xmlOutputDir).listFiles();
            int i = 1;
            while (i < ficheiros.length && ficheiros[i].getName().equals(xmlName + i + ".xml")) {
                i++;
            }
            String xmlFinal = "" + xmlName + i + ".xml";
            StreamResult result = new StreamResult(new File(xmlOutputDir + xmlFinal));
            transformer.transform(source, result);
            return xmlFinal;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException | IOException | SAXException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}

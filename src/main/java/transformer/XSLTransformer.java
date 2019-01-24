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
import java.io.*;

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
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    if (name.contains(".xml")) return true;
                    return false;
                }
            };
            File[] ficheiros = new File(xmlOutputDir).listFiles(filter);
            int count = 1;
            System.out.println("\nCOUNT_NUMERO_FICHEIRO: " + count + "\n");
            for (int i = 0; i < ficheiros.length; i++) {
                if (ficheiros[i].getName().contains(xmlName + count))
                    count++;
            }
            System.out.println("\nCOUNT_NUMERO_FICHEIRO: " + count + "\n");
            String xmlFinal = "" + xmlName + count + ".xml";
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

package restApp;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Building a RESTful Web Service retrieved from:
 * https://spring.io/guides/gs/rest-service/#scratch
 * https://spring.io/guides/gs/rest-service/#scratch Additionally see:
 * https://spring.io/guides/gs/accessing-mongodb-data-rest/
 */
@RestController
public class RESTController {

    @RequestMapping("/getStoreSaleByDate")
    public String getStoreSaleByDate(@RequestParam(value = "store") String storeID,
                                     @RequestParam(value = "month") String month,
                                     @RequestParam(value = "year") String year) {
        MongoConnector mongo = new MongoConnector();
        return mongo.getSaleDetails(storeID, month, year);
        /* Deixei ficar este Catch aqui como IssueTracking!
        -> Basicamente eu estava a pensar que iamos utilizar os inputs em formato numerico
        mas depois descobri que só vamos usar a string query, mais nada (no método aggregate)

        } catch (NumberFormatException exc) {
            return exc.getMessage() +
                    "\nParametro introduzido não corresponde a um número inteiro!";
        }
        */
    }

    /*
    Os métodos daqui para baixo são só para "Quality of Life" do utilizador.
    Também são convenientes para fazer testes, mas o mesmo pode ser feito diretamente no mongoDB!
     */

    @RequestMapping("/isCollectionExistent")
    public String isCollectionExistent(@RequestParam(value = "DB") String db,
                                       @RequestParam(value = "collection") String clct) {
        MongoConnector mongo = new MongoConnector();
        if (mongo.isCollection(db, clct))
            return "Collection exists!";
        else return "Collection does not exist!";
    }

    @RequestMapping("/getCollectionField")
    public String getCollectionField(@RequestParam(value = "DB", defaultValue = "bikeOnTrackDB") String db,
                                     @RequestParam(value = "collection", defaultValue = "salesDetails") String clct,
                                     @RequestParam(value = "field") String field,
                                     @RequestParam(value = "value") String value) {
        MongoConnector mongo = new MongoConnector();
        return mongo.getFieldData(db, clct, field, value);
    }

    @RequestMapping("/aggregateCollectionByQueryString")
    public String aggregateCollectionByQueryString(@RequestParam(value = "DB", defaultValue = "bikeOnTrackDB") String db,
                                                   @RequestParam(value = "collection", defaultValue = "salesDetails") String clct,
                                                   @RequestParam(value = "query") String query) {
        //Example: %5B%7B%24group%3A%7B%22_id%22%3Anull%2Ccount%3A%7B%24sum%3A1%7D%7D%7D%5D 
        //para [{$group:{"_id":null,count:{$sum:1}}}] -> utilizar: https://meyerweb.com/eric/tools/dencoder/
        MongoConnector mongo = new MongoConnector();
        return mongo.aggregateDataByQueryString(db, clct, query);
    }
}

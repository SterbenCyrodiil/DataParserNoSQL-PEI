package app;

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
public class CollectionController {

    @RequestMapping("/isCollection")
    public boolean isCollection(@RequestParam(value="DB") String db,
                                @RequestParam(value="collection") String clct) {
        MongoConnector mongo = new MongoConnector();
        boolean res = mongo.isCollection(db, clct);
        return res;
    }

    @RequestMapping("/getCollectionField")
    public String getCollectionField(@RequestParam(value="DB") String db,
            @RequestParam(value="collection") String clct, @RequestParam(value="field") String field,
            @RequestParam(value = "value") String value) {
        MongoConnector mongo = new MongoConnector();
        String res = mongo.getFieldData(db, clct, field, value);
        return res;
    }

    @RequestMapping("/aggregateCollectionByQueryString")
    public String aggregateCollectionByQueryString(@RequestParam(value="DB") String db,
            @RequestParam(value="Collection") String clct, @RequestParam(value = "query") String query) {
        //Example: %5B%7B%24group%3A%7B%22_id%22%3Anull%2Ccount%3A%7B%24sum%3A1%7D%7D%7D%5D 
        //para [{$group:{"_id":null,count:{$sum:1}}}] -> utilizar: https://meyerweb.com/eric/tools/dencoder/
        MongoConnector mongo = new MongoConnector();
        String res = mongo.aggregateDataByQueryString(db, clct, query);
        return res;
    }
}

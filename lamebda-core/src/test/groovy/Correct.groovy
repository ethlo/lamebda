import com.ethlo.lamebda.HttpRequest
import com.ethlo.lamebda.HttpResponse
import com.ethlo.lamebda.HttpStatus
import com.ethlo.lamebda.SimpleServerFunction;

class Correct extends SimpleServerFunction {
    @Override
    void get(HttpRequest request, HttpResponse response) {
        response.json(HttpStatus.OK, [method: request.method, message: 'Hello world'])
    }
}
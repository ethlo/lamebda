import com.ethlo.lamebda.*;

class Incorrect extends SimpleServerFunction {

    @Override
    get(HttpRequest request, HttpResponse response) {
        response.json(HttpStatus.OK, [method: request.method, message: 'Hello world'])
    }
}
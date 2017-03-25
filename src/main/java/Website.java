import com.google.common.collect.ImmutableMap;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import spark.ModelAndView;
import spark.template.freemarker.FreeMarkerEngine;

import java.io.File;
import java.util.function.Consumer;

import static spark.Spark.get;

public class Website {


    public static void main(String[] args) {
        get("/", (request, response) -> withState(state -> {
            // modify state here
            state.count++;
        }));
    }

















    private static String withState(Consumer<State> consumer) {
        return withState(consumer, false);
    }

    private static String withState(Consumer<State> consumer, boolean isRetry) {
        File file = new File("file.db");
        try (DB db = DBMaker
                .fileDB(file)
                .fileMmapEnable()
                .make()) {
            Atomic.Var<Object> atomic = db
                    .atomicVar("state")
                    .createOrOpen();
            State state = (State) atomic.get();
            if (state == null) {
                state = new State();
            }
            consumer.accept(state);
            atomic.set(state);


            // inspired by http://freemarker.org/docs/pgui_quickstart_createconfiguration.html
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
            configuration.setClassForTemplateLoading(FreeMarkerEngine.class, "");
            configuration.setDefaultEncoding("UTF-8");
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
            BeansWrapperBuilder wrapperBuilder = new BeansWrapperBuilder(Configuration.VERSION_2_3_23);
            wrapperBuilder.setExposeFields(true);
            wrapperBuilder.setExposureLevel(BeansWrapper.EXPOSE_ALL);
            BeansWrapper beansWrapper = wrapperBuilder.build();
            configuration.setObjectWrapper(beansWrapper);
            configuration.setSharedVariable("statics", beansWrapper.getStaticModels());
            configuration.setNumberFormat("computer"); // Don't display numbers with thousands separators by default



            return new FreeMarkerEngine(configuration).render(
                    new ModelAndView(ImmutableMap.of("state", state), "webpage.html"));
        } catch (Throwable t) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            if (!isRetry) {
                return withState(consumer, true);
            } else {
                throw new RuntimeException(t);
            }
        }
    }
}


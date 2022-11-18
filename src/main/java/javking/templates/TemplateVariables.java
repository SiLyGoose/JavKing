package javking.templates;

import com.google.common.base.Joiner;

import java.util.HashMap;

public class TemplateVariables {
    private static final HashMap<Class, TemplateVariableParser> mapper = new HashMap<>();

    static {
        init();
    }

    public String args = null;
    public String[] arg = {null, null, null};

    private static void init() {
        mapper.put(String.class, (var, object) -> {
            if (var.args == null) {
                var.args = (String) object;
            }
            for (int i = 0; i < var.arg.length; i++) {
                if (var.arg[i] == null) {
                    var.arg[i] = (String) object;
                    break;
                }
            }
        });
        mapper.put(String[].class, (var, object) -> var.args = Joiner.on(" ").join((String[]) object));
    }

    public static TemplateVariables create(Object... vars) {
        if (vars == null || vars.length == 0) {
            return new TemplateVariables();
        }
        TemplateVariables templateVariables = new TemplateVariables();
        for (Object var : vars) {
            if (var == null) {
                continue;
            }
            if (mapper.containsKey(var.getClass())) {
                mapper.get(var.getClass()).apply(templateVariables, var);
            }
        }
        return templateVariables;
    }

    private interface TemplateVariableParser {
        void apply(TemplateVariables var, Object o);
    }
}

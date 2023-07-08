package javking.templates;

public class Template {
    final private TemplateArgument[] templateArguments;
    final private TemplateArgument[] optionalArgs;

    public Template(TemplateArgument... templateArguments) {
        this(templateArguments, null);
    }

    public Template(TemplateArgument[] requiredArguments, TemplateArgument[] optionalArgs) {
        if (requiredArguments == null) {
            templateArguments = new TemplateArgument[]{};
        } else {
            templateArguments = requiredArguments;
        }
        if (optionalArgs == null) {
            this.optionalArgs = new TemplateArgument[]{};
        } else {
            this.optionalArgs = optionalArgs;
        }
    }

    public String formatFull(Object... vars) {
        if (templateArguments.length == 0 && optionalArgs.length == 0) {
            return "";
        }
        TemplateVariables env = TemplateVariables.create(vars);
        StringBuilder stringBuilder = new StringBuilder();
        if (templateArguments.length > 0) {
            for (TemplateArgument arg : templateArguments) {
                stringBuilder.append(arg.parse(env)).append(" ");
            }
        }
        if (optionalArgs.length > 0) {
            for (TemplateArgument arg : optionalArgs) {
                String var = arg.parse(env);
                if (!var.isEmpty()) {
                    stringBuilder.append(var).append(" ");
                }
            }
        }
        return stringBuilder.toString();
    }
}

package javking.commands.interceptors;

import javking.models.meta.AbstractCommand;

public interface CommandInterceptor {
    void intercept(AbstractCommand command);
}

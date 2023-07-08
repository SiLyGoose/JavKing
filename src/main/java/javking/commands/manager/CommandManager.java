package javking.commands.manager;

import javking.JavKing;
import javking.commands.interceptors.CommandExecutionInterceptor;
import javking.concurrent.CommandExecutionTask;
import javking.concurrent.ThreadExecutionQueue;
import javking.discord.MessageService;
import javking.exceptions.RateLimitException;
import javking.models.command.CommandContext;
import javking.models.meta.AbstractCommand;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.TreeMap;

public class CommandManager {
    private final TreeMap<String, AbstractCommand> commands = new TreeMap<>();
    private final TreeMap<String, AbstractCommand> commandsAliases = new TreeMap<>();

    private CommandExecutionInterceptor interceptor;

    public void init(MessageService messageService) {
        loadCommands();
        loadAliases();
        loadInterceptor(messageService);
    }

    public TreeMap<String, AbstractCommand> getCommands() {
        return commands;
    }

    public TreeMap<String, AbstractCommand> getAliases() {
        return commandsAliases;
    }

    private AbstractCommand getCommand(String commandName) {
        return commands.get(commandName) == null ? commandsAliases.get(commandName) : commands.get(commandName);
    }

    public void executeCommand(String commandName, ThreadExecutionQueue executionQueue) {
        executeCommand(getCommand(commandName), executionQueue);
    }

    public void executeCommand(String commandName, CommandContext context, ThreadExecutionQueue executionQueue) {
        AbstractCommand command = getCommand(commandName);

        if (command == null) return;

        command.setContext(context);
        command.setCommandBody(context.getCommandBody());
        command.setIdentifier(commandName);

        executeCommand(command, executionQueue);
    }

    public void executeCommand(AbstractCommand command, ThreadExecutionQueue executionQueue) {
        CommandContext context = command.getContext();

        CommandExecutionTask commandExecutionTask = new CommandExecutionTask(command, executionQueue, () -> {
            try {
                interceptor.intercept(command);
            } catch (Throwable e) {
                System.out.println(e.getClass().getSimpleName());
                if (!(e instanceof NullPointerException)) {
                    e.printStackTrace();
                }
            }
        });

        commandExecutionTask.setUncaughtExceptionHandler();
        commandExecutionTask.setName("javking command execution: " + context);

        try {
            boolean queued = !executionQueue.add(commandExecutionTask);

            if (!queued) {
//                too many commands added, current command queued
            }
        } catch (RateLimitException e) {
            if (!e.isTimeout()) {
//                throw new InvalidCommandException(e.getMessage());
                e.printStackTrace();
                System.out.println("Not a command!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCommands() {
        loadCommands("javking.commands");
        loadCommands("javking.commands.admin");
    }

    private void loadCommands(String dir) {
        Reflections reflections = new Reflections(dir);
        Set<Class<? extends AbstractCommand>> classes = reflections.getSubTypesOf(AbstractCommand.class);
        for (Class<? extends AbstractCommand> s : classes) {
            try {
                if (Modifier.isAbstract(s.getModifiers())) {
                    continue;
                }
                AbstractCommand c = s.getConstructor().newInstance();
                String identifier = c.getClass().getSimpleName();
                if (!commands.containsKey(identifier)) commands.put(identifier, c);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadAliases() {
        for (AbstractCommand command : commands.values()) {
            for (String alias : command.getAlias()) {
                if (!commandsAliases.containsKey(alias)) {
                    commandsAliases.put(alias, command);
                }
            }
        }
    }

    private void loadInterceptor(MessageService messageService) {
        interceptor = new CommandExecutionInterceptor(messageService);
    }
}

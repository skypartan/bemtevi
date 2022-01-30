package dev.skypartan.bemtevi;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class Program implements EventListener {

    @NonNull private final String token;
    @Getter private JDA bot;
    @Getter private AudioController audioController;

    private JDABuilder builder;


    @Builder(toBuilder = true)
    public Program(@NotNull String token) {
        this.token = token;
        setup();
    }

    private void setup() {
        System.out.println("Initializing services");
        audioController = new AudioController(this);

        System.out.println("Creating client");
        builder = JDABuilder.create(token,
                GatewayIntent.GUILD_VOICE_STATES,
                GatewayIntent.GUILD_MESSAGES
                /*GatewayIntent.GUILD_MEMBERS*/);

        System.out.println("Registering evens");
        builder.addEventListeners(this);
    }

    public void start() throws LoginException {
        try {
            System.out.println("Connecting client");
            bot = builder.build();
            System.out.println("Client connected");
        }
        catch (LoginException exception) {
            System.err.printf("Client failed to connect. %s%n", exception.getLocalizedMessage());
            throw exception;
        }

        var guilds = bot.getGuilds().stream()
                .map(Guild::getName)
                .reduce((subtotal, element) -> subtotal.concat(", ").concat(element));

        guilds.ifPresentOrElse(
                s -> System.out.printf("Current guilds: [%s]%n", s),
                () -> System.out.println("No current guild"));
    }

    @Override
    public void onEvent(@NotNull GenericEvent genericEvent) {
        // if (genericEvent instanceof MessageReceivedEvent) {
        //     var event = (MessageReceivedEvent) genericEvent;
        //
        //     if (event.getMessage().getContentRaw().equalsIgnoreCase("b.bemtevi")) {
        //         try {
        //             audioController.playSound(event.getGuild(), event.getMember().getVoiceState().getChannel(), audioController.BEMTEVI_ESTOURADO);
        //         }
        //         catch (IOException e) {
        //             e.printStackTrace();
        //         }
        //     }
        // }

        if (!(genericEvent instanceof GuildVoiceJoinEvent) && !(genericEvent instanceof GuildVoiceMoveEvent))
            return;

        var event = (GenericGuildVoiceUpdateEvent) genericEvent;
        if (event.getMember().getUser().isBot())
            return;

        audioController.triggerAction(event);
    }


    public static void main(String[] args) {
        var bot = Program.builder()
                .token(System.getenv("DISCORD_TOKEN"))
                .build();

        try {
            bot.start();
        }
        catch (LoginException e) {
            e.printStackTrace();
        }
    }
}

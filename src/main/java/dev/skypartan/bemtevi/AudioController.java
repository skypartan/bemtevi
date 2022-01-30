package dev.skypartan.bemtevi;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;

public class AudioController {

    public static final String BEMTEVI = "bemtevi.mp3";
    public static final String BEMTEVI_ESTOURADO = "bemtevi-estourado.mp3";
    public static final String O_KAWAII_KOTO = "bemtevi-estourado.mp3";

    @NonNull private final Program bot;
    @Getter private DefaultAudioPlayerManager audioPlayerManager;

    private final Random random = new Random();
    private AudioPlayer player;


    public AudioController(@NonNull Program bot) {
        this.bot = bot;

        audioPlayerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(audioPlayerManager);
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
    }


    public void triggerAction(GenericGuildVoiceUpdateEvent event) {
        try {
            var value = random.nextInt(0, 101) % 100.0;
            System.out.printf("%f value at %s -> %s\n", value, event.getGuild().getName(), event.getChannelJoined().getName());
            if (value > 95) {
                playSound(event.getGuild(), event.getChannelJoined(), BEMTEVI_ESTOURADO);
            }
            else if (value > 70) {
                playSound(event.getGuild(), event.getChannelJoined(), BEMTEVI);
            }
        }
        catch (IOException ignored) {}
    }

    public void playSound(Guild guild, VoiceChannel channel, String sound) throws IOException {
        final var manager = guild.getAudioManager();
        if (manager.isConnected())
            return;

        player = audioPlayerManager.createPlayer();

        manager.setSendingHandler(new AudioPlayerSendHandler(player));
        manager.openAudioConnection(channel);
        player.addListener(event -> {
            // System.out.printf("event %s\n", event.getClass());

            if (event instanceof TrackEndEvent || manager.isConnected())
                manager.closeAudioConnection();
        });

        var audioPath = Paths.get("assets", sound);
        audioPlayerManager.loadItem(audioPath.toAbsolutePath().toString(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                // System.out.printf("trackLoaded %s\n", track.getInfo().title);

                player.setVolume(100);
                player.playTrack(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                // System.out.printf("playlistLoaded %s\n", playlist.getName());
            }

            @Override
            public void noMatches() {
                // System.out.println("noMatches");
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                // System.out.println("loadFailed");
                exception.printStackTrace();
            }
        });
    }
}

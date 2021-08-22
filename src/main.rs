use std::env;
use rand::Rng;
use songbird::{Event, EventContext, EventHandler as VoiceEventHandler, SerenityInit, TrackEvent, Songbird};
use serenity::async_trait;
use serenity::client::Context;
use serenity::client::{Client, EventHandler};
use serenity::framework::StandardFramework;
use serenity::framework::standard::macros::group;
use serenity::model::gateway::Ready;
use serenity::model::id::{GuildId, ChannelId};
use serenity::model::prelude::VoiceState;
use serenity::http::Http;
use std::sync::Arc;

struct Handler;

#[async_trait]
impl EventHandler for Handler {
    async fn ready(&self, _ctx: Context, ready: Ready) {
        println!("{} is connected!", ready.user.name);

        // for guild in ready.guilds {
        //     match context.cache.guild(guild.id()) {
        //         Some(guild) => {  },
        //         None => {}
        //     }
        // }
        // println!("Present in {}", ready.guilds);
    }

    async fn voice_state_update(&self, ctx: Context, _guild: Option<GuildId>,
                                old_state: Option<VoiceState>, new_state: VoiceState) {

        let user_id = new_state.member.as_ref()
            .expect("Falha ao obter usuário")
            .user.id;

        if (old_state.is_none() || old_state.unwrap().channel_id != new_state.channel_id)
            && new_state.channel_id.is_some()
            && ctx.cache.current_user().await.id != user_id {

            let guild_id = new_state.guild_id.expect("Falha ao obter guild");
            let channel_id = new_state.channel_id.expect("Falha ao obter canal");

            let should_join = rand::thread_rng().gen_range(1, 10) > 7;
            println!("User {:?} connecting to {:?} -> {:?}. Join = {:?}",
                     user_id, guild_id, channel_id, should_join);

            if should_join {
                join_channel(&ctx, &new_state).await;
                if new_state.guild_id.is_some() {
                    play_audio(&ctx, &new_state).await;
                }
            }
        }
    }
}

struct SongEndNotifier {
    channel_id: ChannelId,
    guild_id: GuildId,
    http: Arc<Http>,
    manager: Arc<Songbird>,
}

#[async_trait]
impl VoiceEventHandler for SongEndNotifier {
    async fn act(&self, _ctx: &EventContext<'_>) -> Option<Event> {
        let _ = self.manager.remove(self.guild_id).await;
        None
    }
}

#[group]
struct General;

#[tokio::main]
async fn main() {
    let token = env::var("DISCORD_TOKEN")
        .expect("Expected a token in the environment");

    let framework = StandardFramework::new()
        .configure(|c| c.prefix("~"))
        .group(&GENERAL_GROUP);

    println!("Creating client");
    let mut client = Client::builder(&token)
        .event_handler(Handler)
        .framework(framework)
        .register_songbird()
        .await
        .expect("Error creating client");

    let _ = client.start().await.map_err(|why| println!("Client ended: {:?}", why));
}

async fn join_channel(ctx: &Context, state: &VoiceState) {
    let manager = songbird::get(ctx).await
        .expect("Songbird Voice client placed in at initialisation.")
        .clone();

    let guild_id = state.guild_id.unwrap();
    let channel_id = state.channel_id.unwrap();

    // Evitar entrar junto com o miaaau
    let members = ctx.http.get_channel(channel_id.0)
        .await
        .unwrap()
        .guild()
        .unwrap()
        .members(&ctx.cache)
        .await
        .unwrap();

    for member in members {
        if member.user.id.0 == 690555384464670822 {
            let _ = member.disconnect_from_voice(&ctx.http).await;
        }
    }

    let (handle_lock, success) = manager.join(guild_id, channel_id).await;

    if let Ok(_channel) = success {
        let send_http = ctx.http.clone();
        let mut handle = handle_lock.lock().await;

        handle.add_global_event(
            Event::Track(TrackEvent::End),
            SongEndNotifier {
                channel_id,
                guild_id,
                http: send_http,
                manager,
            },
        );
    }
}

async fn play_audio(ctx: &Context, state: &VoiceState) {
    let guild_id = state.guild_id.unwrap();

    let manager = songbird::get(ctx).await
        .expect("Songbird Voice client placed in at initialisation.")
        .clone();

    if let Some(handler_lock) = manager.get(guild_id) {
        let mut handler = handler_lock.lock().await;

        match songbird::ffmpeg("res/audio.wav").await {
            Ok(source) => {
                let _ = handler.play_source(source);
            }
            Err(why) => {
                println!("Error reading source: {:?}", why);
            }
        };
    }
}

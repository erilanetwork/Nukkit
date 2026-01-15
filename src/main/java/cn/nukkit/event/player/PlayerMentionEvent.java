package cn.nukkit.event.player;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;

public class PlayerMentionEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final Player mentioned;
    private String message;

    public PlayerMentionEvent(Player mentioner, Player mentioned, String message) {
        this.player = mentioner;
        this.mentioned = mentioned;
        this.message = message;
    }

    public Player getMentioner() {
        return this.player;
    }

    public Player getMentioned() {
        return this.mentioned;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

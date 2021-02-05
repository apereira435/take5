import kotlinx.coroutines.*


interface Mattermost {

    class message {
        var from: String
        var message : String
        var channel : String
        init {
            from = ""
            message = ""
            channel = ""
        }
    }

    fun connect(ip: String, port: String): Boolean
    fun login(username: String, password: String): Boolean
    fun sendPrivateMessage(m : String, user : String) : Boolean
    fun sendPublicMessage(m : String, channel : String) : Boolean
    fun setPrivateMessageCallback(callback: (m : message) -> Unit)
    fun setPublicMessageCallback(callback: (m : message) -> Unit)
    fun createPrivateMessageRoom(users : MutableList<String>) : String?
    fun sendMessageByRoomID(m : String, roomID : String ) : Boolean
    fun getBotChannel() : String
}

interface LoggingSystem {
    fun debug(message: String)
    fun error(message : String)
    fun info(message : String)
}

interface Game {
    fun createGameRoomUrl() : String?
    fun isItPossibleToBeSinglePlayer() : Boolean
    fun getName() : String
    fun possibleToJoinInMidGame() : Boolean
}

typealias Games = MutableList<Game>
typealias Lobby = Pair<Game, MutableList<String>>

class Take5Bot(ip: String, port: String, driver : Mattermost, loggingSystem: LoggingSystem, game : Game) {

    // 30 seconds timeout for when a player wants to play
    val TIMEOUT_FIRST_PLAYER_MS = 30_000L
    val TIMEOUT_EXTRA_PLAYER_MS = 30_000L

    companion object {
        val username = "take5"
        val password = "password"
    }

    class Lobby {
        var users = mutableListOf<String>()
        lateinit var gameRoomUrl : String
        lateinit var chatRoomID : String
    }

    enum class State {
        waiting,
        waiting_for_second_player,
        gameroom_created,
        added_new_player,
        lock_room
    }

    enum class Action {
        wants_to_play,
        wants_to_chill,
        timeout
    }

    var state = State.waiting
    var ip = ip
    var port = port
    var mm = driver
    var log = loggingSystem
    var lobby = Lobby()
    var game = game

    init {

    }

    fun handle_waiting_state(action : Action, m: Mattermost.message) {
        mm.sendPrivateMessage("Understood, waiting for new players ...", m.from)
        mm.sendPublicMessage("Someone wants to play and is waiting ...", mm.getBotChannel())
        // create a new lobby
        lobby.users.add(m.from)
        state = State.waiting_for_second_player
        // we need to start a timeout event for this action
        GlobalScope.launch {
            delay(TIMEOUT_FIRST_PLAYER_MS)
            handle_action(Action.timeout, Mattermost.message())
        }
    }

    fun handle_waiting_for_second_player(action : Action, m: Mattermost.message) {
        if (action == Action.timeout) {
            if (game.isItPossibleToBeSinglePlayer())
            // first player
            mm.sendPrivateMessage(lobby.users[0],
                "No body has joined yet, will create a room to play individually")

        } else if (action == Action.wants_to_play && m.from != lobby.users[0]) {
            val gameRoomUrl = game.createGameRoomUrl()
            if (gameRoomUrl != null) {
                lobby.gameRoomUrl = gameRoomUrl
                val chatRoomID = mm.createPrivateMessageRoom(lobby.users)
                if (chatRoomID == null) {
                    // error exists at creating game room
                    // TODO: add missing business logic here
                    return
                }
                lobby.chatRoomID = chatRoomID
                mm.sendMessageByRoomID(lobby.chatRoomID, "hello, you have been paired to play"
                + game.getName())
                if (game.possibleToJoinInMidGame()) {
                    mm.sendMessageByRoomID(lobby.chatRoomID,
                                        "This game it is possible to have someone join in mid game")
                }
                mm.sendMessageByRoomID(lobby.chatRoomID,
                                        "here is the link for the game lobby " + lobby.gameRoomUrl)
                mm.sendMessageByRoomID(lobby.chatRoomID, "enjoy")
                // if it is possible to join in mid game
                // next state is to wait for players to join in mid game
                // else it goes back to start
                if (game.possibleToJoinInMidGame()) {
                    state = State.gameroom_created
                    GlobalScope.launch {
                        delay(TIMEOUT_EXTRA_PLAYER_MS)
                        handle_action(Action.timeout, Mattermost.message())
                    }
                }
                else {
                    state = State.waiting
                }
            }
        }
    }

    fun handle_ameroom_created(action : Action, m: Mattermost.message) {
        if (action == Action.timeout) {
            // if timesout when room was created then
            state = State.waiting
        }
    }

    fun handle_action(action : Action, m: Mattermost.message) {
        if (state == State.waiting) {
            handle_waiting_state(action, m)
        } else if (state == State.waiting_for_second_player) {
            handle_waiting_for_second_player(action, m)
        } else if (state == State.gameroom_created) {
            handle_ameroom_created(action, m)
        }
    }

    fun onPrivateMessageSent(m: Mattermost.message) {

    }

    fun onPublicMessageSent(m: Mattermost.message) {

    }

    fun start() {
        log.debug("logging in")
        if (!mm.connect(ip, port)) {
            log.error("could not connect!");
            return;
        }

        if (!mm.login(username, password)) {
            log.error("could not login!");
            return;
        }


        log.info("logged in")

        mm.setPrivateMessageCallback(::onPrivateMessageSent)
        mm.setPublicMessageCallback(::onPublicMessageSent)
    }
}

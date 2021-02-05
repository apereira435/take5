import io.mockk.*
import kotlin.test.Test

class Take5BotTest {
    @Test
    fun test_it_connects() {
        var mattermostDriver = mockk<Mattermost>(relaxed = true)
        var loggingSystem = mockk<LoggingSystem>(relaxed = true)
        var game = mockk<Game>(relaxed = true)
        val host = "localhost"
        val port = "4900"
        var bot = Take5Bot(host, port, mattermostDriver, loggingSystem, game)
        every  {mattermostDriver.connect(any(), any())}.returns(true);
        bot.start()
        verify {mattermostDriver.connect(host, port)}
        verify {mattermostDriver.login(Take5Bot.username, Take5Bot.password)}
    }

    @Test
    fun test_it_sends_message
}
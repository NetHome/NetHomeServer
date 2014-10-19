package nu.nethome.home.items.net;

import nu.nethome.home.items.net.Message;
import nu.nethome.home.system.Event;
import nu.nethome.home.system.HomeService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xmpp.Jid;
import org.xmpp.TcpConnection;
import org.xmpp.XmppSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class XmppClientTest {

    XmppClient client;
    private HomeService server;
    private Event event;
    private XmppSession session;

    @Before
    public void setUp() throws Exception {
        client = spy(new XmppClient());
        session = mock(XmppSession.class);
        doReturn(session).when(client).createSession();
        server = mock(HomeService.class);
        event = mock(Event.class);
        doReturn(Message.MESSAGE_TYPE).when(event).getAttribute(Event.EVENT_TYPE_ATTRIBUTE);
        doReturn("xmpp:a@b").when(event).getAttribute(Message.TO);
        doReturn("subject").when(event).getAttribute(Message.SUBJECT);
        doReturn("text").when(event).getAttribute(Message.BODY);
        doReturn(Message.OUT_BOUND).when(event).getAttribute(Message.DIRECTION);
    }

    @Test
    public void sendsXmppMessage() throws Exception {
        client.activate(server);
        client.receiveEvent(event);

        ArgumentCaptor<Jid> jidArgumentCaptor = ArgumentCaptor.forClass(Jid.class);
        ArgumentCaptor<org.xmpp.stanza.client.Message> messageArgumentCaptor = ArgumentCaptor.forClass(org.xmpp.stanza.client.Message.class);
        verify(session).send(messageArgumentCaptor.capture());

        assertThat(messageArgumentCaptor.getValue().getTo().getDomain(), is("b"));
        assertThat(messageArgumentCaptor.getValue().getTo().getLocal(), is("a"));

    }
}

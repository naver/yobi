package controllers;

import models.*;
import org.junit.*;
import play.test.FakeApplication;
import play.test.Helpers;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static play.test.Helpers.callAction;
import static play.test.Helpers.fakeRequest;

public class SiteAppTest {
    protected static FakeApplication app;
    private User admin;
    private User notAdmin;

    @BeforeClass
    public static void beforeClass() {
        callAction(
                routes.ref.Application.init()
        );
    }

    @Before
    public void before() {
        Map<String, String> config = support.Config.makeTestConfig();
        config.put("signup.require.confirm", "true");
        config.put("application.secret", "foo");
        app = Helpers.fakeApplication(config);
        Helpers.start(app);
    }

    @After
    public void after() {
        Helpers.stop(app);
    }

    @Test @Ignore   //FixMe I don't know how to make a assert
    public void testToggleUserAccountLock() {
        //Given
        Map<String,String> data = new HashMap<>();
        final String loginId= "doortts";
        data.put("loginId", loginId);

        User targetUser = User.findByLoginId(loginId);
        System.out.println(targetUser.isLocked);
        boolean currentIsLocked = targetUser.isLocked;

        //When
        callAction(
                controllers.routes.ref.SiteApp.toggleAccountLock(loginId),
                fakeRequest()
                        .withFormUrlEncodedBody(data)
                        .withSession("loginId", "admin")
        );
        //Then
        assertThat(User.findByLoginId(loginId).isLocked).isNotEqualTo(currentIsLocked);
    }
}

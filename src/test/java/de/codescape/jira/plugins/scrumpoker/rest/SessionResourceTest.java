package de.codescape.jira.plugins.scrumpoker.rest;

import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import de.codescape.jira.plugins.scrumpoker.ao.ScrumPokerSession;
import de.codescape.jira.plugins.scrumpoker.rest.entities.SessionEntity;
import de.codescape.jira.plugins.scrumpoker.service.EstimationFieldService;
import de.codescape.jira.plugins.scrumpoker.service.ScrumPokerSessionService;
import de.codescape.jira.plugins.scrumpoker.service.SessionEntityTransformer;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class SessionResourceTest {

    private static final String ISSUE_KEY = "ISSUE-1";
    private static final String USER_KEY = "userKey";
    private static final String CARD_VALUE = "5";
    private static final int ESTIMATION = 5;

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private ScrumPokerSessionService scrumPokerSessionService;

    @Mock
    private EstimationFieldService estimationFieldService;

    @Mock
    private SessionEntityTransformer sessionEntityTransformer;

    @InjectMocks
    private SessionResource sessionResource;

    @Mock
    private ApplicationUser applicationUser;

    @Mock
    private ScrumPokerSession scrumPokerSession;

    @Mock
    private SessionEntity sessionEntity;

    @Test
    public void getSessionShouldReturnSessionForTheGivenIssueKey() {
        expectCurrentUserIs(USER_KEY);
        expectCurrentSessionForUser(ISSUE_KEY, USER_KEY);

        SessionEntity sessionEntity = (SessionEntity) sessionResource.getSession(ISSUE_KEY).getEntity();

        assertThat(sessionEntity.getIssueKey(), is(equalTo(ISSUE_KEY)));
    }

    @Test
    public void updateCardShouldUpdateTheCardForTheCurrentUser() {
        expectCurrentUserIs(USER_KEY);
        expectCurrentSessionForUser(ISSUE_KEY, USER_KEY);

        sessionResource.updateCard(ISSUE_KEY, CARD_VALUE);

        verify(scrumPokerSessionService).addVote(ISSUE_KEY, USER_KEY, CARD_VALUE);
    }

    @Test
    public void revealingCardShouldRevealCardsForUnderlyingSession() {
        expectCurrentUserIs(USER_KEY);
        expectCurrentSessionForUser(ISSUE_KEY, USER_KEY);

        sessionResource.revealCards(ISSUE_KEY);

        verify(scrumPokerSessionService, times(1)).reveal(ISSUE_KEY, USER_KEY);
    }

    @Test
    public void resettingSessionShouldResetTheUnderlyingSession() {
        expectCurrentUserIs(USER_KEY);
        expectCurrentSessionForUser(ISSUE_KEY, USER_KEY);

        sessionResource.resetSession(ISSUE_KEY);

        verify(scrumPokerSessionService, times(1)).reset(ISSUE_KEY, USER_KEY);
    }

    @Test
    public void confirmingEstimationShouldConfirmEstimationInUnderlyingSessionAndPersistEstimation() {
        expectCurrentUserIs(USER_KEY);
        expectCurrentSessionForUser(ISSUE_KEY, USER_KEY);

        sessionResource.confirmEstimation(ISSUE_KEY, ESTIMATION);

        verify(scrumPokerSessionService, times(1)).confirm(ISSUE_KEY, USER_KEY, ESTIMATION);
        verify(estimationFieldService, times(1)).save(ISSUE_KEY, ESTIMATION);
    }

    private void expectCurrentUserIs(String userKey) {
        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(applicationUser);
        when(applicationUser.getKey()).thenReturn(userKey);
    }

    private void expectCurrentSessionForUser(String issueKey, String userKey) {
        when(scrumPokerSessionService.byIssueKey(issueKey, userKey)).thenReturn(scrumPokerSession);
        when(scrumPokerSession.getIssueKey()).thenReturn(issueKey);
        when(sessionEntityTransformer.build(scrumPokerSession, USER_KEY)).thenReturn(sessionEntity);
        when(sessionEntity.getIssueKey()).thenReturn(ISSUE_KEY);
    }

}

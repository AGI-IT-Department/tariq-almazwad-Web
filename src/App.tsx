import React, { useState } from 'react';
import { Loader2 } from 'lucide-react';
import { useChallengeApp } from './hooks/useChallengeApp';
import { AppHeader } from './components/AppHeader';
import { BottomNav } from './components/BottomNav';
import { InstallPwaBanner } from './components/InstallPwaBanner';
import { ConfirmLogoutModal, ToastHost } from './components/SharedUi';
import { RemindersModal } from './components/RemindersModal';
import { WelcomeScreen } from './screens/WelcomeScreen';
import { LoginScreen } from './screens/LoginScreen';
import { DailyTasksScreen } from './screens/DailyTasksScreen';
import { ReadingPlanScreen } from './screens/ReadingPlanScreen';
import { MyLogScreen } from './screens/MyLogScreen';
import { MyRankScreen } from './screens/MyRankScreen';
import { AdminDashboardScreen } from './screens/AdminDashboardScreen';

export const App: React.FC = () => {
  const app = useChallengeApp();
  const [showReminders, setShowReminders] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const showYouthShell =
    !app.isLoading && !app.isAdminOpen && app.hasSeenWelcome && Boolean(app.activeUser);

  return (
    <div dir="rtl" className="min-h-[100dvh] bg-parchment text-ink-deep">
      <ToastHost toast={app.toast} />

      {showReminders && (
        <RemindersModal
          settings={app.reminderSettings}
          onUpdateSettings={app.updateReminders}
          onSendTestBible={app.sendTestBibleReminder}
          onSendTest={app.sendTestEncouragement}
          onDismiss={() => setShowReminders(false)}
        />
      )}

      {showLogoutConfirm && app.activeUser && (
        <ConfirmLogoutModal
          userName={app.activeUser.fullName}
          onConfirm={() => {
            setShowLogoutConfirm(false);
            app.logoutYouth();
          }}
          onCancel={() => setShowLogoutConfirm(false)}
        />
      )}

      {app.isLoading ? (
        <div className="flex min-h-[100dvh] items-center justify-center bg-parchment">
          <Loader2 className="h-10 w-10 animate-spin text-gold-warm" />
        </div>
      ) : app.isAdminOpen ? (
        <AdminDashboardScreen
          isUnlocked={app.isAdminUnlocked}
          pendingSubmissions={app.pendingSubmissions}
          leaderboard={app.leaderboard}
          allUsersCount={app.allUsers.length}
          approvedSubmissionsCount={
            app.allSubmissions.filter((s) => s.status === 'approved').length
          }
          allTasks={app.allTasks}
          appConfig={app.appConfig}
          onUnlock={app.unlockAdmin}
          onClose={app.closeAdmin}
          onLock={app.lockAdmin}
          onReviewSubmission={app.reviewSubmission}
          onApproveAllPending={app.approveAllPending}
          onAdjustBonus={app.adjustUserBonus}
          onAdminAddUser={app.adminAddUser}
          onAdminUpdateUser={app.adminUpdateUser}
          onAdminDeleteUser={app.adminDeleteUser}
          onUpdateTask={app.updateTask}
          onUpdateConfig={app.updateAppConfig}
          onToggleQuizWeek={app.toggleQuizWeek}
          onExportCsv={app.exportLeaderboardCsv}
          onChangeAdminPin={app.changeAdminPin}
          onFeedback={app.showToast}
        />
      ) : !app.hasSeenWelcome ? (
        <WelcomeScreen onStartJourney={app.completeWelcome} />
      ) : !app.activeUser ? (
        <LoginScreen
          isSyncing={app.isSyncing}
          isSupabaseLive={app.isSupabaseLive}
          onLoginOrRegister={app.loginOrRegister}
          onOpenAdmin={app.openAdmin}
        />
      ) : (
        <>
          <AppHeader
            isSupabaseLive={app.isSupabaseLive}
            isSyncing={app.isSyncing}
            pendingAdminCount={app.isAdminUnlocked ? app.pendingSubmissions.length : 0}
            onOpenAdmin={app.openAdmin}
            onOpenReminders={() => setShowReminders(true)}
            onRefresh={app.refreshNow}
            onLogout={() => setShowLogoutConfirm(true)}
          />

          <main>
            {app.selectedTab === 'daily' && (
              <DailyTasksScreen
                user={app.activeUser}
                rankEntry={app.activeUserRankEntry}
                allTasks={app.allTasks}
                userSubmissions={app.userSubmissions}
                appConfig={app.appConfig}
                reminderSettings={app.reminderSettings}
                onSubmitTask={app.submitTask}
                onSubmitAutoQuiz={app.submitAutoQuizResult}
                onOpenReadingPlan={() => app.setSelectedTab('plan')}
                onOpenMyRank={() => app.setSelectedTab('my_rank')}
                onOpenReminders={() => setShowReminders(true)}
                onLogout={() => setShowLogoutConfirm(true)}
                onFeedback={app.showToast}
              />
            )}
            {app.selectedTab === 'plan' && (
              <ReadingPlanScreen
                userId={app.activeUser.id}
                allTasks={app.allTasks}
                userSubmissions={app.userSubmissions}
                appConfig={app.appConfig}
                onSubmitTask={app.submitTask}
                onSubmitAutoQuiz={app.submitAutoQuizResult}
              />
            )}
            {app.selectedTab === 'my_log' && (
              <MyLogScreen submissions={app.userSubmissions} />
            )}
            {app.selectedTab === 'my_rank' && (
              <MyRankScreen
                user={app.activeUser}
                rankEntry={app.activeUserRankEntry}
                totalParticipants={app.leaderboard.length}
              />
            )}
          </main>

          <BottomNav
            selectedTab={app.selectedTab}
            onSelectTab={app.setSelectedTab}
          />
        </>
      )}

      {/* Smart dismissible Arabic PWA Install Banner */}
      <InstallPwaBanner aboveBottomNav={showYouthShell} />
    </div>
  );
};

export default App;

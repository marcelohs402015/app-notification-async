import { useEffect, useState } from "react";
import { motion } from "framer-motion";
import { LogOut, Users, Bell } from "lucide-react";
import { useNavigate } from "react-router-dom";
import toast from "react-hot-toast";
import { useAuthStore } from "../store/auth-store";
import { useNotificationStore } from "../store/notification-store";
import { NotificationBell } from "../components/notification-bell";
import { UserCard } from "../components/user-card";
import { SendNotificationForm } from "../components/send-notification-form";
import { NotificationList } from "../components/notification-list";
import { useSSE } from "../hooks/use-sse";
import apiClient from "../lib/api-client";
import type { User, Notification, PageResponse } from "../types";

export const DashboardPage = () => {
  const navigate = useNavigate();
  const { user: currentUser, logout } = useAuthStore();
  const { setNotifications } = useNotificationStore();
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [isLoadingUsers, setIsLoadingUsers] = useState(true);
  const [activeTab, setActiveTab] = useState<"send" | "history">("send");

  useSSE();

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        const [usersRes, notificationsRes] = await Promise.all([
          apiClient.get<User[]>("/users"),
          apiClient.get<PageResponse<Notification>>("/notifications?page=0&size=50"),
        ]);
        setUsers(usersRes.data);
        const unreadCount = notificationsRes.data.content.filter((n) => !n.read).length;
        setNotifications(notificationsRes.data.content, unreadCount);
      } catch {
        toast.error("Failed to load data.");
      } finally {
        setIsLoadingUsers(false);
      }
    };
    loadInitialData();
  }, [setNotifications]);

  const handleLogout = () => {
    logout();
    navigate("/login");
  };

  const initials = currentUser?.username.slice(0, 2).toUpperCase() ?? "??";

  return (
    <div className="min-h-screen bg-slate-950">
      <header className="sticky top-0 z-40 bg-slate-900/80 backdrop-blur-md border-b border-slate-700/50">
        <div className="max-w-7xl mx-auto px-4 h-14 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-7 h-7 bg-blue-600 rounded-lg flex items-center justify-center">
              <Bell className="w-4 h-4 text-white" />
            </div>
            <span className="font-bold text-white text-sm">AppNotification</span>
          </div>

          <div className="flex items-center gap-3">
            <NotificationBell />

            <div className="flex items-center gap-2 pl-3 border-l border-slate-700">
              <div className="w-7 h-7 bg-blue-600 rounded-full flex items-center justify-center text-xs font-bold text-white">
                {initials}
              </div>
              <span className="text-sm text-slate-300 hidden sm:block">{currentUser?.username}</span>
            </div>

            <button
              onClick={handleLogout}
              aria-label="Sign out"
              tabIndex={0}
              className="p-1.5 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 py-6">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.4 }}
            className="lg:col-span-1"
          >
            <div className="bg-slate-900 border border-slate-700 rounded-2xl overflow-hidden">
              <div className="px-4 py-3 border-b border-slate-700 flex items-center gap-2">
                <Users className="w-4 h-4 text-slate-400" />
                <h2 className="text-sm font-semibold text-white">Users</h2>
                {!isLoadingUsers && (
                  <span className="ml-auto text-xs text-slate-500">{users.length} online</span>
                )}
              </div>

              <div className="p-3 space-y-2 max-h-[calc(100vh-200px)] overflow-y-auto">
                {isLoadingUsers ? (
                  <div className="space-y-2">
                    {Array.from({ length: 4 }).map((_, i) => (
                      <div key={i} className="h-16 bg-slate-800 rounded-xl animate-pulse" />
                    ))}
                  </div>
                ) : users.length === 0 ? (
                  <p className="text-sm text-slate-500 text-center py-8">No other users found</p>
                ) : (
                  users.map((u) => (
                    <UserCard
                      key={u.id}
                      user={u}
                      isSelected={selectedUser?.id === u.id}
                      onSelect={setSelectedUser}
                    />
                  ))
                )}
              </div>
            </div>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.4, delay: 0.1 }}
            className="lg:col-span-2"
          >
            <div className="bg-slate-900 border border-slate-700 rounded-2xl overflow-hidden">
              <div className="flex border-b border-slate-700">
                {(["send", "history"] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    aria-label={tab === "send" ? "Send notification" : "Notification history"}
                    tabIndex={0}
                    className={`
                      flex-1 py-3 text-sm font-medium capitalize transition-colors
                      ${activeTab === tab
                        ? "text-white border-b-2 border-blue-500"
                        : "text-slate-400 hover:text-slate-200"
                      }
                    `}
                  >
                    {tab === "send" ? "Send Notification" : "My Notifications"}
                  </button>
                ))}
              </div>

              <div className="p-5">
                {activeTab === "send" ? (
                  <motion.div
                    key="send"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.25 }}
                  >
                    {!selectedUser && (
                      <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="mb-4 p-3 bg-slate-800/50 border border-slate-700 rounded-xl text-sm text-slate-400 text-center"
                      >
                        Select a user from the list to send them a notification
                      </motion.div>
                    )}
                    {selectedUser && (
                      <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="mb-4 flex items-center gap-3 p-3 bg-blue-600/10 border border-blue-500/30 rounded-xl"
                      >
                        <div className="w-8 h-8 bg-blue-600 rounded-full flex items-center justify-center text-xs font-bold text-white">
                          {selectedUser.username.slice(0, 2).toUpperCase()}
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-white">{selectedUser.username}</p>
                          <p className="text-xs text-slate-400">{selectedUser.email}</p>
                        </div>
                      </motion.div>
                    )}
                    <SendNotificationForm selectedUser={selectedUser} />
                  </motion.div>
                ) : (
                  <motion.div
                    key="history"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.25 }}
                  >
                    <NotificationList />
                  </motion.div>
                )}
              </div>
            </div>
          </motion.div>
        </div>
      </main>
    </div>
  );
};

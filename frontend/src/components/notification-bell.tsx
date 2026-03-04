import { useEffect, useRef } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Bell, X, CheckCheck } from "lucide-react";
import { useNotificationStore } from "../store/notification-store";
import { NotificationList } from "./notification-list";
import apiClient from "../lib/api-client";

export const NotificationBell = () => {
  const { unreadCount, isOpen, togglePanel, closePanel, markAllAsRead, notifications } =
    useNotificationStore();
  const panelRef = useRef<HTMLDivElement>(null);
  const bellRef = useRef<HTMLButtonElement>(null);
  const prevUnreadRef = useRef(unreadCount);
  const isShakingRef = useRef(false);

  useEffect(() => {
    if (unreadCount > prevUnreadRef.current && !isShakingRef.current) {
      isShakingRef.current = true;
      bellRef.current?.classList.add("animate-shake");
      setTimeout(() => {
        bellRef.current?.classList.remove("animate-shake");
        isShakingRef.current = false;
      }, 600);
    }
    prevUnreadRef.current = unreadCount;
  }, [unreadCount]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(event.target as Node)) {
        closePanel();
      }
    };
    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
    }
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [isOpen, closePanel]);

  const handleMarkAllRead = async () => {
    const unread = notifications.filter((n) => !n.read);
    await Promise.all(unread.map((n) => apiClient.patch(`/notifications/${n.id}/read`)));
    markAllAsRead();
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      togglePanel();
    }
  };

  return (
    <div className="relative" ref={panelRef}>
      <button
        ref={bellRef}
        onClick={togglePanel}
        onKeyDown={handleKeyDown}
        aria-label={`Notifications${unreadCount > 0 ? `, ${unreadCount} unread` : ""}`}
        aria-expanded={isOpen}
        aria-haspopup="dialog"
        tabIndex={0}
        className="relative p-2 text-slate-400 hover:text-white hover:bg-slate-700 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
      >
        <Bell className="w-5 h-5" />
        <AnimatePresence>
          {unreadCount > 0 && (
            <motion.span
              key="badge"
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              exit={{ scale: 0 }}
              transition={{ type: "spring", stiffness: 400, damping: 20 }}
              className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-red-500 rounded-full flex items-center justify-center text-[10px] font-bold text-white px-1"
            >
              {unreadCount > 99 ? "99+" : unreadCount}
            </motion.span>
          )}
        </AnimatePresence>
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            key="panel"
            initial={{ opacity: 0, scale: 0.95, y: -10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: -10 }}
            transition={{ duration: 0.18, ease: "easeOut" }}
            role="dialog"
            aria-label="Notifications panel"
            className="absolute right-0 top-12 w-96 bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl shadow-black/50 overflow-hidden z-50"
          >
            <div className="flex items-center justify-between px-4 py-3 border-b border-slate-700">
              <h2 className="text-sm font-semibold text-white">Notifications</h2>
              <div className="flex items-center gap-1">
                {unreadCount > 0 && (
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={handleMarkAllRead}
                    aria-label="Mark all as read"
                    tabIndex={0}
                    className="flex items-center gap-1 text-xs text-blue-400 hover:text-blue-300 px-2 py-1 rounded-md hover:bg-slate-700 transition-colors"
                  >
                    <CheckCheck className="w-3.5 h-3.5" />
                    Mark all read
                  </motion.button>
                )}
                <button
                  onClick={closePanel}
                  aria-label="Close notifications"
                  tabIndex={0}
                  className="p-1 text-slate-400 hover:text-white hover:bg-slate-700 rounded-md transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>
            <NotificationList />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

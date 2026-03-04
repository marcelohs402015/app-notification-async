import { AnimatePresence, motion } from "framer-motion";
import { BellOff, Info, AlertTriangle, CheckCircle, XCircle } from "lucide-react";
import { useNotificationStore } from "../store/notification-store";
import type { Notification, NotificationType } from "../types";
import apiClient from "../lib/api-client";
import { formatDistanceToNow } from "../lib/date-utils";

const NOTIFICATION_ICONS: Record<NotificationType, React.ReactNode> = {
  INFO: <Info className="w-4 h-4 text-blue-400" />,
  WARNING: <AlertTriangle className="w-4 h-4 text-yellow-400" />,
  SUCCESS: <CheckCircle className="w-4 h-4 text-green-400" />,
  ERROR: <XCircle className="w-4 h-4 text-red-400" />,
};

const NOTIFICATION_COLORS: Record<NotificationType, string> = {
  INFO: "border-l-blue-500",
  WARNING: "border-l-yellow-500",
  SUCCESS: "border-l-green-500",
  ERROR: "border-l-red-500",
};

interface NotificationItemProps {
  notification: Notification;
  index: number;
}

const NotificationItem = ({ notification, index }: NotificationItemProps) => {
  const markAsRead = useNotificationStore((state) => state.markAsRead);

  const handleMarkRead = async () => {
    if (notification.read) return;
    await apiClient.patch(`/notifications/${notification.id}/read`);
    markAsRead(notification.id);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      handleMarkRead();
    }
  };

  return (
    <motion.div
      layout
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      exit={{ opacity: 0, x: -20, height: 0 }}
      transition={{
        layout: { duration: 0.2 },
        opacity: { duration: 0.2, delay: index * 0.04 },
        x: { type: "spring", stiffness: 300, damping: 25, delay: index * 0.04 },
      }}
      onClick={handleMarkRead}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      role="button"
      aria-label={`Notification from ${notification.sender.username}: ${notification.message}`}
      className={`
        flex gap-3 p-3 border-l-2 cursor-pointer transition-colors rounded-r-lg
        ${NOTIFICATION_COLORS[notification.type]}
        ${notification.read ? "opacity-50" : "bg-slate-800/50 hover:bg-slate-800"}
      `}
    >
      <div className="flex-shrink-0 mt-0.5">
        {NOTIFICATION_ICONS[notification.type]}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-0.5">
          <span className="text-xs font-semibold text-slate-300 truncate">
            {notification.sender.username}
          </span>
          {!notification.read && (
            <motion.span
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              className="w-1.5 h-1.5 bg-blue-500 rounded-full flex-shrink-0"
            />
          )}
        </div>
        <p className="text-sm text-slate-200 line-clamp-2 leading-snug">{notification.message}</p>
        <p className="text-xs text-slate-500 mt-1">{formatDistanceToNow(notification.createdAt)}</p>
      </div>
    </motion.div>
  );
};

export const NotificationList = () => {
  const notifications = useNotificationStore((state) => state.notifications);

  if (notifications.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
        <BellOff className="w-10 h-10 text-slate-600 mb-3" />
        <p className="text-sm text-slate-400 font-medium">No notifications yet</p>
        <p className="text-xs text-slate-500 mt-1">Notifications will appear here</p>
      </div>
    );
  }

  return (
    <div className="max-h-[420px] overflow-y-auto py-2 px-2 space-y-1 scrollbar-thin scrollbar-track-slate-900 scrollbar-thumb-slate-700">
      <AnimatePresence initial={false}>
        {notifications.map((notification, index) => (
          <NotificationItem key={notification.id} notification={notification} index={index} />
        ))}
      </AnimatePresence>
    </div>
  );
};

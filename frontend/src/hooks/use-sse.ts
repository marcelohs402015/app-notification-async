import { useEffect, useRef } from "react";
import toast from "react-hot-toast";
import { useAuthStore } from "../store/auth-store";
import { useNotificationStore } from "../store/notification-store";
import type { Notification } from "../types";

const NOTIFICATION_TYPE_ICONS: Record<string, string> = {
  INFO: "ℹ️",
  WARNING: "⚠️",
  SUCCESS: "✅",
  ERROR: "❌",
};

export const useSSE = () => {
  const token = useAuthStore((state) => state.token);
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const addNotification = useNotificationStore((state) => state.addNotification);
  const eventSourceRef = useRef<EventSource | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !token) return;

    const url = `/api/notifications/stream?token=${token}`;
    const eventSource = new EventSource(url);
    eventSourceRef.current = eventSource;

    eventSource.addEventListener("notification", (event) => {
      const notification: Notification = JSON.parse(event.data);
      addNotification(notification);
      const icon = NOTIFICATION_TYPE_ICONS[notification.type] ?? "🔔";
      toast(`${icon} ${notification.sender.username}: ${notification.message}`, {
        duration: 5000,
      });
    });

    eventSource.onerror = () => {
      eventSource.close();
      eventSourceRef.current = null;
    };

    return () => {
      eventSource.close();
      eventSourceRef.current = null;
    };
  }, [isAuthenticated, token, addNotification]);
};

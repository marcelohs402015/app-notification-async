import { useState } from "react";
import { motion } from "framer-motion";
import { Send, Loader2 } from "lucide-react";
import toast from "react-hot-toast";
import apiClient from "../lib/api-client";
import type { NotificationType, User } from "../types";

interface SendNotificationFormProps {
  selectedUser: User | null;
}

const NOTIFICATION_TYPES: { value: NotificationType; label: string; color: string }[] = [
  { value: "INFO", label: "Info", color: "bg-blue-600" },
  { value: "SUCCESS", label: "Success", color: "bg-green-600" },
  { value: "WARNING", label: "Warning", color: "bg-yellow-600" },
  { value: "ERROR", label: "Error", color: "bg-red-600" },
];

export const SendNotificationForm = ({ selectedUser }: SendNotificationFormProps) => {
  const [message, setMessage] = useState("");
  const [type, setType] = useState<NotificationType>("INFO");
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUser || !message.trim()) return;

    setIsLoading(true);
    try {
      await apiClient.post("/notifications/send", {
        recipientId: selectedUser.id,
        message: message.trim(),
        type,
      });
      toast.success(`Notification sent to ${selectedUser.username}!`);
      setMessage("");
    } catch {
      toast.error("Failed to send notification.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <div>
        <label className="block text-sm font-medium text-slate-300 mb-2">Notification type</label>
        <div className="flex gap-2 flex-wrap">
          {NOTIFICATION_TYPES.map(({ value, label, color }) => (
            <motion.button
              key={value}
              type="button"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setType(value)}
              aria-pressed={type === value}
              aria-label={`${label} notification type`}
              tabIndex={0}
              className={`
                px-3 py-1.5 rounded-lg text-xs font-semibold transition-all border
                ${type === value
                  ? `${color} text-white border-transparent`
                  : "bg-slate-800 text-slate-400 border-slate-600 hover:border-slate-400"
                }
              `}
            >
              {label}
            </motion.button>
          ))}
        </div>
      </div>

      <div>
        <label htmlFor="message" className="block text-sm font-medium text-slate-300 mb-1.5">
          Message
        </label>
        <textarea
          id="message"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Type your notification message..."
          maxLength={500}
          rows={3}
          required
          className="w-full bg-slate-800 border border-slate-600 rounded-xl px-4 py-3 text-white placeholder-slate-500 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-all resize-none"
          aria-label="Notification message"
        />
        <p className="text-xs text-slate-500 text-right mt-1">{message.length}/500</p>
      </div>

      <motion.button
        whileHover={{ scale: selectedUser ? 1.02 : 1 }}
        whileTap={{ scale: selectedUser ? 0.98 : 1 }}
        type="submit"
        disabled={!selectedUser || !message.trim() || isLoading}
        aria-label="Send notification"
        className="w-full bg-blue-600 hover:bg-blue-500 disabled:bg-slate-700 disabled:cursor-not-allowed text-white disabled:text-slate-500 font-semibold py-2.5 px-4 rounded-xl transition-colors flex items-center justify-center gap-2"
      >
        {isLoading ? (
          <>
            <Loader2 className="w-4 h-4 animate-spin" />
            Sending...
          </>
        ) : (
          <>
            <Send className="w-4 h-4" />
            {selectedUser ? `Send to ${selectedUser.username}` : "Select a user first"}
          </>
        )}
      </motion.button>
    </form>
  );
};

import { motion } from "framer-motion";
import type { User } from "../types";

interface UserCardProps {
  user: User;
  isSelected: boolean;
  onSelect: (user: User) => void;
}

export const UserCard = ({ user, isSelected, onSelect }: UserCardProps) => {
  const handleClick = () => onSelect(user);
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      onSelect(user);
    }
  };

  const initials = user.username.slice(0, 2).toUpperCase();
  const hue = user.id.charCodeAt(0) * 37 % 360;

  return (
    <motion.button
      layout
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      onClick={handleClick}
      onKeyDown={handleKeyDown}
      tabIndex={0}
      aria-label={`Select user ${user.username}`}
      aria-pressed={isSelected}
      className={`
        w-full flex items-center gap-3 p-3 rounded-xl border transition-all text-left
        ${isSelected
          ? "bg-blue-600/20 border-blue-500 shadow-md shadow-blue-500/10"
          : "bg-slate-800/50 border-slate-700 hover:border-slate-500"
        }
      `}
    >
      <div
        className="w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold text-white flex-shrink-0"
        style={{ background: `hsl(${hue}, 60%, 45%)` }}
      >
        {initials}
      </div>
      <div className="flex-1 min-w-0">
        <p className="text-sm font-semibold text-white truncate">{user.username}</p>
        <p className="text-xs text-slate-400 truncate">{user.email}</p>
      </div>
      {isSelected && (
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          className="w-2 h-2 bg-blue-400 rounded-full flex-shrink-0"
        />
      )}
    </motion.button>
  );
};

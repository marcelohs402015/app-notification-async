export type NotificationType = "INFO" | "WARNING" | "SUCCESS" | "ERROR";

export interface User {
  id: string;
  username: string;
  email: string;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Notification {
  id: string;
  sender: User;
  message: string;
  type: NotificationType;
  read: boolean;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface SendNotificationRequest {
  recipientId: string;
  message: string;
  type: NotificationType;
}

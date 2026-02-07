// User types
export interface User {
  id: number;
  email: string;
  phone: string;
  fullName: string;
  avatarUrl?: string;
  whatsappLink?: string;
  profileVisibility: ProfileVisibility;
  hideFromExecutorList: boolean;
  emailVerified: boolean;
  phoneVerified: boolean;
  executorVerified: boolean;
  role: UserRole;
  active: boolean;
  createdAt: string;
}

export type ProfileVisibility = 'PUBLIC' | 'PRIVATE';
export type UserRole = 'USER' | 'ADMIN';

// Auth types
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  phone: string;
  password: string;
  fullName: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

// Executor types
export interface ExecutorListItem {
  id: number;
  fullName: string;
  avatarUrl?: string;
  bio?: string;
  specialization?: string;
  completedOrders: number;
  rating: number;
  reviewCount: number;
  availableForWork: boolean;
  categories: Category[];
}

export interface ExecutorProfile {
  id: number;
  fullName: string;
  avatarUrl?: string;
  whatsappLink?: string;
  bio?: string;
  specialization?: string;
  totalOrders: number;
  completedOrders: number;
  avgCompletionDays?: number;
  rating: number;
  reviewCount: number;
  availableForWork: boolean;
  lastActiveAt?: string;
  memberSince?: string;
  categories: Category[];
}

export interface ExecutorProfileRequest {
  bio?: string;
  specialization?: string;
  availableForWork: boolean;
  categoryIds: number[];
}

// Category types
export interface Category {
  id: number;
  name: string;
  slug: string;
  description?: string;
  iconUrl?: string;
  parentId?: number;
  children?: Category[];
  orderCount?: number;
  executorCount?: number;
  active: boolean;
}

// Order types
export type OrderStatus = 'NEW' | 'IN_PROGRESS' | 'REVISION' | 'ON_REVIEW' | 'COMPLETED' | 'DISPUTED' | 'CANCELLED';

// Order list response (from /api/v1/orders)
export interface OrderListItem {
  id: number;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  clientId: number;
  clientName: string;
  budgetMin?: number;
  budgetMax?: number;
  deadline?: string;
  location?: string;
  status: OrderStatus;
  responseCount: number;
  createdAt: string;
}

// Order detail response (from /api/v1/orders/:id)
export interface OrderDetail {
  id: number;
  title: string;
  description: string;
  categoryId: number;
  categoryName: string;
  clientId: number;
  clientName: string;
  clientAvatarUrl?: string;
  executorId?: number;
  executorName?: string;
  executorAvatarUrl?: string;
  budgetMin?: number;
  budgetMax?: number;
  agreedPrice?: number;
  location?: string;
  deadline?: string;
  agreedDeadline?: string;
  status: OrderStatus;
  isPublic: boolean;
  viewCount: number;
  responseCount: number;
  attachments: string[];
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  isOwner: boolean;
  isExecutor: boolean;
  hasResponded: boolean;
  descriptionTruncated?: boolean;
  requiresVerification?: boolean;
}

// Legacy Order type (for backwards compatibility)
export interface Order {
  id: number;
  title: string;
  description: string;
  category: Category;
  client: User;
  executor?: User;
  budgetMin?: number;
  budgetMax?: number;
  agreedPrice?: number;
  deadline?: string;
  agreedDeadline?: string;
  status: OrderStatus;
  viewCount: number;
  responseCount: number;
  attachments: string[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderCreateRequest {
  title: string;
  description: string;
  categoryId: number;
  budgetMin?: number;
  budgetMax?: number;
  deadline?: string;
  location?: string;
  attachments?: string[];
}

export interface OrderResponse {
  id: number;
  orderId: number;
  executorId: number;
  executorName: string;
  executorAvatarUrl?: string;
  executorSpecialization?: string;
  executorRating: number;
  executorCompletedOrders: number;
  coverLetter: string;
  proposedPrice?: number;
  proposedDays?: number;
  isSelected: boolean;
  createdAt: string;
}

export interface OrderResponseRequest {
  coverLetter: string;
  proposedPrice?: number;
  proposedDays?: number;
}

// Review types
export interface Review {
  id: number;
  orderId: number;
  orderTitle: string;
  clientId: number;
  clientName: string;
  clientAvatarUrl?: string;
  rating: number;
  comment?: string;
  createdAt: string;
}

export interface ReviewCreateRequest {
  rating: number;
  comment?: string;
}

// Portfolio types
export interface PortfolioItem {
  id: number;
  title: string;
  description?: string;
  imageUrls: string[];
  projectUrl?: string;
  category?: Category;
  createdAt: string;
}

export interface PortfolioCreateRequest {
  title: string;
  description?: string;
  imageUrls: string[];
  projectUrl?: string;
  categoryId?: number;
}

// Chat types
export interface ChatRoom {
  id: number;
  orderId: number;
  orderTitle: string;
  participantId: number;
  participantName: string;
  participantAvatarUrl?: string;
  lastMessage?: string;
  lastMessageAt?: string;
  lastMessageSenderId?: number;
  unreadCount: number;
  createdAt: string;
}

export interface Message {
  id: number;
  chatRoomId: number;
  senderId: number;
  senderName: string;
  senderAvatarUrl?: string;
  content: string;
  attachments?: string[];
  isRead: boolean;
  isMine: boolean;
  read: boolean; // alias for isRead
  createdAt: string;
}

export interface SendMessageRequest {
  content: string;
  attachments?: string[];
}

// Pagination
export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;  // Spring uses 'number' but some endpoints may use 'page'
  page?: number;   // Alternative field name for page number
  first: boolean;
  last: boolean;
}

// Filters
export interface OrderFilters {
  categoryId?: number;
  status?: OrderStatus;
  minBudget?: number;
  maxBudget?: number;
  search?: string;
  location?: string;
}

export interface ExecutorFilters {
  categoryId?: number;
  minRating?: number;
  availableOnly?: boolean;
  search?: string;
}

// Verification types
export type VerificationStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED';

export interface VerificationResponse {
  status: VerificationStatus;
  submittedAt?: string;
  reviewedAt?: string;
  rejectionReason?: string;
}

export interface VerificationSubmitRequest {
  passportUrl: string;
  selfieUrl: string;
}

export interface AdminVerificationResponse {
  userId: number;
  userFullName: string;
  userEmail: string;
  userPhone: string;
  userAvatarUrl?: string;
  status: VerificationStatus;
  passportUrl: string;
  selfieUrl: string;
  rejectionReason?: string;
  submittedAt: string;
  reviewedAt?: string;
  reviewedByName?: string;
}

export interface FileUploadResponse {
  url: string;
}

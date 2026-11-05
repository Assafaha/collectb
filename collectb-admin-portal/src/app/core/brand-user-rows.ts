import { BrandUser } from './models';

export interface BrandUserRow {
  userId: string;
  userEmail: string;
  userDisplayName: string;
  roleName: string;
  assignedAt: string;
  assignments: BrandUser[];
}

export function groupBrandUsers(assignments: BrandUser[]): BrandUserRow[] {
  const users = new Map<string, BrandUserRow>();
  for (const assignment of assignments) {
    let user = users.get(assignment.userId);
    if (!user) {
      user = {
        userId: assignment.userId,
        userEmail: assignment.userEmail,
        userDisplayName: assignment.userDisplayName,
        roleName: '',
        assignedAt: assignment.assignedAt,
        assignments: [],
      };
      users.set(assignment.userId, user);
    }
    user.assignments.push(assignment);
    if (Date.parse(assignment.assignedAt) > Date.parse(user.assignedAt)) {
      user.assignedAt = assignment.assignedAt;
    }
    user.roleName = [...new Set(user.assignments.map((role) => role.roleName))].join(', ');
  }
  return [...users.values()];
}

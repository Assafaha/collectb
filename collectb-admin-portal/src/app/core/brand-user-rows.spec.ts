import { groupBrandUsers } from './brand-user-rows';
import { BrandUser } from './models';

describe('groupBrandUsers', () => {
  const viewer: BrandUser = {
    userId: 'viewer', userEmail: 'viewer@example.com', userDisplayName: 'Viewer',
    roleAssignmentId: 'viewer-role', roleName: 'BRAND_VIEWER', assignedAt: '2026-11-01T08:00:00Z',
  };

  it('groups multiple roles into one user while preserving individual assignment IDs', () => {
    const editor = {
      ...viewer, roleAssignmentId: 'editor-role', roleName: 'BRAND_EDITOR', assignedAt: '2026-11-01T09:00:00Z',
    };
    const rows = groupBrandUsers([viewer, editor]);
    expect(rows).toHaveLength(1);
    expect(rows[0].roleName).toBe('BRAND_VIEWER, BRAND_EDITOR');
    expect(rows[0].assignedAt).toBe(editor.assignedAt);
    expect(rows[0].assignments.map((role) => role.roleAssignmentId)).toEqual(['viewer-role', 'editor-role']);
  });

  it('keeps distinct users separate and updates after an assignment is revoked', () => {
    const other = { ...viewer, userId: 'other', roleAssignmentId: 'other-role' };
    expect(groupBrandUsers([viewer, other])).toHaveLength(2);
    expect(groupBrandUsers([viewer])[0].assignments).toEqual([viewer]);
    expect(groupBrandUsers([])).toEqual([]);
  });
});

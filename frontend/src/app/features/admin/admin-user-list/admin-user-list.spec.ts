import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AdminUserList } from './admin-user-list';

describe('AdminUserList', () => {
  let component: AdminUserList;
  let fixture: ComponentFixture<AdminUserList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminUserList],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminUserList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});

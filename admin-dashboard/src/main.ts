import { CommonModule } from '@angular/common';
import { Component, Injectable, OnDestroy, OnInit, signal } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import Keycloak from 'keycloak-js';

type Overview = {
  totalVideos: number;
  uploadedVideos: number;
  processingVideos: number;
  completedVideos: number;
  failedVideos: number;
};

type Video = {
  id: string;
  originalFilename: string;
  ownerUsername: string;
  fileSize: number;
  status: string;
  durationSeconds: number | null;
  originalPath: string;
  thumbnailPath: string | null;
  processedPath: string | null;
  createdAt: string;
  updatedAt: string;
};

type Job = {
  id: string;
  videoId: string;
  status: string;
  inputPath: string;
  outputPath: string | null;
  thumbnailPath: string | null;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
};

type PresignedUrl = {
  url: string;
  expiresAt: string;
};

type AssetType = 'original' | 'thumbnail' | 'processed';

const localHostnames = new Set(['localhost', '127.0.0.1']);
const isLocalBrowser = localHostnames.has(window.location.hostname);
const apiBaseUrl = isLocalBrowser ? 'http://localhost:8081/api' : `https://api.vidio.md-dev970.com/api`;
const keycloakUrl = isLocalBrowser ? 'http://localhost:8089' : 'https://vidio.md-dev970.com';

@Injectable({ providedIn: 'root' })
class AuthService {
  private readonly keycloak = new Keycloak({
    url: keycloakUrl,
    realm: 'vidio',
    clientId: 'vidio-dashboard'
  });

  readonly ready = signal(false);
  readonly authenticated = signal(false);
  readonly username = signal('');
  readonly isAdmin = signal(false);

  async init(): Promise<void> {
    const authenticated = await this.keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256'
    });
    this.applyAuthState(authenticated);
  }

  login(): void {
    void this.keycloak.login();
  }

  logout(): void {
    void this.keycloak.logout({ redirectUri: window.location.origin });
  }

  async token(): Promise<string> {
    await this.keycloak.updateToken(30);
    return this.keycloak.token ?? '';
  }

  private applyAuthState(authenticated: boolean): void {
    this.authenticated.set(authenticated);
    this.username.set(this.keycloak.tokenParsed?.['preferred_username'] as string ?? '');
    const roles = (this.keycloak.tokenParsed?.['realm_access'] as { roles?: string[] } | undefined)?.roles ?? [];
    this.isAdmin.set(roles.includes('ADMIN'));
    this.ready.set(true);
  }
}

@Injectable({ providedIn: 'root' })
class UserApi {
  private readonly baseUrl = `${apiBaseUrl}/videos`;

  constructor(private readonly auth: AuthService) {}

  async videos(): Promise<Video[]> {
    return this.get<Video[]>('');
  }

  async upload(file: File): Promise<Video> {
    const token = await this.auth.token();
    const body = new FormData();
    body.append('file', file);
    const response = await fetch(this.baseUrl, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body
    });
    if (!response.ok) {
      throw new Error(await errorMessage(response));
    }
    return response.json() as Promise<Video>;
  }

  async assetUrl(videoId: string, assetType: AssetType): Promise<PresignedUrl> {
    return this.get<PresignedUrl>(`/${videoId}/assets/${assetType}/url`);
  }

  private async get<T>(path: string): Promise<T> {
    const token = await this.auth.token();
    const response = await fetch(`${this.baseUrl}${path}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!response.ok) {
      throw new Error(await errorMessage(response));
    }
    return response.json() as Promise<T>;
  }
}

@Injectable({ providedIn: 'root' })
class AdminApi {
  private readonly baseUrl = `${apiBaseUrl}/admin`;

  constructor(private readonly auth: AuthService) {}

  async overview(): Promise<Overview> {
    return this.get<Overview>('/overview');
  }

  async videos(): Promise<Video[]> {
    return this.get<Video[]>('/videos');
  }

  async jobs(): Promise<Job[]> {
    return this.get<Job[]>('/jobs');
  }

  async assetUrl(videoId: string, assetType: AssetType): Promise<PresignedUrl> {
    return this.get<PresignedUrl>(`/videos/${videoId}/assets/${assetType}/url`);
  }

  private async get<T>(path: string): Promise<T> {
    const token = await this.auth.token();
    const response = await fetch(`${this.baseUrl}${path}`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!response.ok) {
      throw new Error(await errorMessage(response));
    }
    return response.json() as Promise<T>;
  }
}

async function errorMessage(response: Response): Promise<string> {
  try {
    const body = await response.json() as { message?: string; error?: string };
    return body.message ?? body.error ?? `Request failed: ${response.status}`;
  } catch {
    return `Request failed: ${response.status}`;
  }
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <header>
      <h1>VidIO</h1>
      <div class="session" *ngIf="auth.ready()">
        <span *ngIf="auth.authenticated()">Signed in as {{ auth.username() }}</span>
        <span class="role" *ngIf="auth.authenticated()">{{ auth.isAdmin() ? 'ADMIN' : 'USER' }}</span>
        <button *ngIf="!auth.authenticated()" (click)="auth.login()">Sign in</button>
        <button *ngIf="auth.authenticated()" (click)="auth.logout()">Sign out</button>
      </div>
    </header>

    <main *ngIf="auth.ready() && auth.authenticated(); else gate">
      <section class="upload-panel">
        <div>
          <h2>Upload Video</h2>
          <p>Choose a video file to upload and process with FFmpeg.</p>
        </div>
        <div class="upload-controls">
          <label class="file-picker">
            <input type="file" accept="video/*" (change)="selectFile($event)">
            <span>{{ selectedFile()?.name || 'Choose file' }}</span>
          </label>
          <button [disabled]="!selectedFile() || uploading()" (click)="uploadSelectedFile()">
            {{ uploading() ? 'Uploading' : 'Upload' }}
          </button>
        </div>
        <p class="error" *ngIf="uploadError()">{{ uploadError() }}</p>
        <p class="success" *ngIf="uploadMessage()">{{ uploadMessage() }}</p>
      </section>

      <section>
        <div class="section-title">
          <h2>My Videos</h2>
          <button [disabled]="loadingMine()" (click)="loadMine()">Refresh</button>
        </div>
        <table *ngIf="myVideos().length; else emptyMine">
          <thead>
            <tr>
              <th>File</th>
              <th>Status</th>
              <th>Size</th>
              <th>Duration</th>
              <th>Outputs</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let video of myVideos()">
              <td>{{ video.originalFilename }}</td>
              <td><span class="status" [class.status-active]="isActive(video.status)">{{ video.status }}</span></td>
              <td>{{ formatBytes(video.fileSize) }}</td>
              <td>{{ formatDuration(video.durationSeconds) }}</td>
              <td>
                <div class="asset-actions">
                  <button *ngIf="video.originalPath" (click)="openUserAsset(video, 'original')">Open original</button>
                  <button *ngIf="video.thumbnailPath" (click)="openUserAsset(video, 'thumbnail')">Open thumbnail</button>
                  <button *ngIf="video.processedPath" (click)="openUserAsset(video, 'processed')">Open processed</button>
                </div>
              </td>
              <td>{{ formatDate(video.updatedAt) }}</td>
            </tr>
          </tbody>
        </table>
        <ng-template #emptyMine>
          <div class="empty">No videos uploaded yet.</div>
        </ng-template>
      </section>

      <ng-container *ngIf="auth.isAdmin()">
        <section class="metrics" *ngIf="overview() as stats">
          <article><strong>{{ stats.totalVideos }}</strong><span>Total</span></article>
          <article><strong>{{ stats.uploadedVideos }}</strong><span>Uploaded</span></article>
          <article><strong>{{ stats.processingVideos }}</strong><span>Processing</span></article>
          <article><strong>{{ stats.completedVideos }}</strong><span>Completed</span></article>
          <article><strong>{{ stats.failedVideos }}</strong><span>Failed</span></article>
        </section>

        <section>
          <div class="section-title">
            <h2>All Videos</h2>
            <button [disabled]="loadingAdmin()" (click)="loadAdmin()">Refresh</button>
          </div>
          <table *ngIf="adminVideos().length; else emptyAdminVideos">
            <thead>
              <tr>
                <th>File</th>
                <th>Owner</th>
                <th>Status</th>
                <th>Size</th>
                <th>Duration</th>
                <th>Outputs</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let video of adminVideos()">
                <td>{{ video.originalFilename }}</td>
                <td>{{ video.ownerUsername }}</td>
                <td><span class="status" [class.status-active]="isActive(video.status)">{{ video.status }}</span></td>
                <td>{{ formatBytes(video.fileSize) }}</td>
                <td>{{ formatDuration(video.durationSeconds) }}</td>
                <td>
                  <div class="asset-actions">
                    <button *ngIf="video.originalPath" (click)="openAdminAsset(video, 'original')">Open original</button>
                    <button *ngIf="video.thumbnailPath" (click)="openAdminAsset(video, 'thumbnail')">Open thumbnail</button>
                    <button *ngIf="video.processedPath" (click)="openAdminAsset(video, 'processed')">Open processed</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyAdminVideos>
            <div class="empty">No videos in the system yet.</div>
          </ng-template>
        </section>

        <section>
          <h2>Processing Jobs</h2>
          <table *ngIf="jobs().length; else emptyJobs">
            <thead>
              <tr>
                <th>Video</th>
                <th>Status</th>
                <th>Input</th>
                <th>Output</th>
                <th>Error</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let job of jobs()">
                <td>{{ job.videoId }}</td>
                <td><span class="status" [class.status-active]="isActive(job.status)">{{ job.status }}</span></td>
                <td>{{ job.inputPath }}</td>
                <td>{{ job.outputPath || '-' }}</td>
                <td>{{ job.errorMessage || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <ng-template #emptyJobs>
            <div class="empty">No processing jobs yet.</div>
          </ng-template>
        </section>
      </ng-container>
    </main>

    <ng-template #gate>
      <main class="gate">
        <p *ngIf="!auth.ready()">Loading session...</p>
        <p *ngIf="auth.ready() && !auth.authenticated()">Sign in to upload videos and track processing.</p>
      </main>
    </ng-template>
  `
})
class AppComponent implements OnInit, OnDestroy {
  readonly overview = signal<Overview | null>(null);
  readonly myVideos = signal<Video[]>([]);
  readonly adminVideos = signal<Video[]>([]);
  readonly jobs = signal<Job[]>([]);
  readonly selectedFile = signal<File | null>(null);
  readonly uploading = signal(false);
  readonly loadingMine = signal(false);
  readonly loadingAdmin = signal(false);
  readonly uploadError = signal('');
  readonly uploadMessage = signal('');
  private refreshHandle: number | undefined;

  constructor(
    readonly auth: AuthService,
    private readonly userApi: UserApi,
    private readonly adminApi: AdminApi
  ) {}

  async ngOnInit(): Promise<void> {
    await this.auth.init();
    if (this.auth.authenticated()) {
      await this.load();
      this.refreshHandle = window.setInterval(() => {
        if (this.hasActiveVideos()) {
          void this.load();
        }
      }, 5000);
    }
  }

  ngOnDestroy(): void {
    if (this.refreshHandle !== undefined) {
      window.clearInterval(this.refreshHandle);
    }
  }

  selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.item(0) ?? null);
    this.uploadError.set('');
    this.uploadMessage.set('');
  }

  async uploadSelectedFile(): Promise<void> {
    const file = this.selectedFile();
    if (!file) {
      return;
    }

    this.uploading.set(true);
    this.uploadError.set('');
    this.uploadMessage.set('');
    try {
      const uploaded = await this.userApi.upload(file);
      this.selectedFile.set(null);
      this.uploadMessage.set(`${uploaded.originalFilename} uploaded and queued for processing.`);
      await this.load();
    } catch (error) {
      this.uploadError.set(error instanceof Error ? error.message : 'Upload failed');
    } finally {
      this.uploading.set(false);
    }
  }

  async load(): Promise<void> {
    await this.loadMine();
    if (this.auth.isAdmin()) {
      await this.loadAdmin();
    }
  }

  async loadMine(): Promise<void> {
    this.loadingMine.set(true);
    try {
      this.myVideos.set(await this.userApi.videos());
    } finally {
      this.loadingMine.set(false);
    }
  }

  async loadAdmin(): Promise<void> {
    this.loadingAdmin.set(true);
    try {
      const [overview, videos, jobs] = await Promise.all([
        this.adminApi.overview(),
        this.adminApi.videos(),
        this.adminApi.jobs()
      ]);
      this.overview.set(overview);
      this.adminVideos.set(videos);
      this.jobs.set(jobs);
    } finally {
      this.loadingAdmin.set(false);
    }
  }

  async openUserAsset(video: Video, assetType: AssetType): Promise<void> {
    await this.openAsset(() => this.userApi.assetUrl(video.id, assetType));
  }

  async openAdminAsset(video: Video, assetType: AssetType): Promise<void> {
    await this.openAsset(() => this.adminApi.assetUrl(video.id, assetType));
  }

  isActive(status: string): boolean {
    return status === 'UPLOADED' || status === 'PROCESSING' || status === 'PENDING';
  }

  formatBytes(value: number): string {
    if (value < 1024) {
      return `${value} B`;
    }
    if (value < 1024 * 1024) {
      return `${(value / 1024).toFixed(1)} KB`;
    }
    return `${(value / 1024 / 1024).toFixed(1)} MB`;
  }

  formatDuration(value: number | null): string {
    return value == null ? '-' : `${value.toFixed(1)}s`;
  }

  formatDate(value: string): string {
    return value ? new Date(value).toLocaleString() : '-';
  }

  private hasActiveVideos(): boolean {
    return this.myVideos().some(video => this.isActive(video.status))
        || this.adminVideos().some(video => this.isActive(video.status))
        || this.jobs().some(job => this.isActive(job.status));
  }

  private async openAsset(urlFactory: () => Promise<PresignedUrl>): Promise<void> {
    try {
      const response = await urlFactory();
      window.open(response.url, '_blank', 'noopener');
    } catch (error) {
      this.uploadError.set(error instanceof Error ? error.message : 'Could not open asset');
    }
  }
}

void bootstrapApplication(AppComponent);

package com.omniroute.app;

import android.content.Context;

import com.omniroute.app.core.AppStrings;
import com.omniroute.app.core.auth.AuthRepository;
import com.omniroute.app.core.auth.TokenStore;
import com.omniroute.app.core.community.CommunityRepository;
import com.omniroute.app.core.data.PlanningSnapshotStore;
import com.omniroute.app.core.data.SettingsStore;
import com.omniroute.app.core.feedback.FeedbackRepository;
import com.omniroute.app.core.map.StaticMapRepository;
import com.omniroute.app.core.network.ApiClient;
import com.omniroute.app.core.budget.BudgetRepository;
import com.omniroute.app.core.notify.NotificationRepository;
import com.omniroute.app.core.photos.PhotoRepository;
import com.omniroute.app.core.planning.PlanningRepository;
import com.omniroute.app.core.planning.PlanningStreamRepository;
import com.omniroute.app.core.room.RoomRepository;
import com.omniroute.app.core.variant.VariantRepository;

/** 手写依赖容器：这个规模的应用不需要引入 DI 框架。 */
public final class AppContainer {

    private final SettingsStore settingsStore;
    private final PlanningSnapshotStore planningSnapshotStore;
    private final AuthRepository authRepository;
    private final PlanningRepository planningRepository;
    private final CommunityRepository communityRepository;
    private final PhotoRepository photoRepository;
    private final BudgetRepository budgetRepository;
    private final RoomRepository roomRepository;
    private final VariantRepository variantRepository;
    private final PlanningStreamRepository planningStreamRepository;
    private final FeedbackRepository feedbackRepository;
    private final NotificationRepository notificationRepository;
    private final StaticMapRepository staticMapRepository;

    public AppContainer(Context context) {
        Context appContext = context.getApplicationContext();
        AppStrings.init(appContext);
        this.settingsStore = new SettingsStore(appContext);
        this.planningSnapshotStore = new PlanningSnapshotStore(appContext);

        TokenStore tokenStore = new TokenStore(appContext);
        ApiClient apiClient = new ApiClient(BuildConfig.BASE_URL, tokenStore);
        this.authRepository = new AuthRepository(apiClient, tokenStore);
        this.planningRepository = new PlanningRepository(apiClient, authRepository);
        this.communityRepository = new CommunityRepository(apiClient);
        this.photoRepository = new PhotoRepository(apiClient);
        this.budgetRepository = new BudgetRepository(apiClient);
        this.roomRepository = new RoomRepository(apiClient, settingsStore, authRepository);
        this.variantRepository = new VariantRepository(apiClient, authRepository);
        this.planningStreamRepository = new PlanningStreamRepository(apiClient, authRepository);
        this.feedbackRepository = new FeedbackRepository(apiClient, authRepository);
        this.notificationRepository = new NotificationRepository(apiClient);
        this.staticMapRepository = new StaticMapRepository(apiClient, appContext);
    }

    public SettingsStore getSettingsStore() {
        return settingsStore;
    }

    public PlanningSnapshotStore getPlanningSnapshotStore() {
        return planningSnapshotStore;
    }

    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    public PlanningRepository getPlanningRepository() {
        return planningRepository;
    }

    public CommunityRepository getCommunityRepository() {
        return communityRepository;
    }

    public PhotoRepository getPhotoRepository() {
        return photoRepository;
    }

    public BudgetRepository getBudgetRepository() {
        return budgetRepository;
    }

    public RoomRepository getRoomRepository() {
        return roomRepository;
    }

    public VariantRepository getVariantRepository() {
        return variantRepository;
    }

    public PlanningStreamRepository getPlanningStreamRepository() {
        return planningStreamRepository;
    }

    public FeedbackRepository getFeedbackRepository() {
        return feedbackRepository;
    }

    public NotificationRepository getNotificationRepository() {
        return notificationRepository;
    }

    public StaticMapRepository getStaticMapRepository() {
        return staticMapRepository;
    }
}

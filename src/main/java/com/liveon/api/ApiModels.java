package com.liveon.api;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON objects exchanged with the Live On API.
 *
 * Fields are deliberately simple and public: Gson can read them without
 * reflection helpers, and contributors can compare each class directly with
 * docs/api.md.
 */
public final class ApiModels
{
    private ApiModels()
    {
    }

    public static final class AuthRequest
    {
        public String rsn;
        public String clanName;
        public String clanRank;
        public String accessCode;
        public String pluginVersion;

        public AuthRequest(String rsn, String clanName, String clanRank, String accessCode, String pluginVersion)
        {
            this.rsn = rsn;
            this.clanName = clanName;
            this.clanRank = clanRank;
            this.accessCode = accessCode;
            this.pluginVersion = pluginVersion;
        }
    }

    public static final class AuthResponse
    {
        public boolean authorized;
        public boolean staff;
        public String token;
        public String role;
        public String loginMessage;
        public String reason;
        public String expiresAt;
    }

    public static final class ItemPayload
    {
        public int itemId;
        public String name;
        public int quantity;
        public long unitPrice;
        public long totalPrice;

        public ItemPayload(int itemId, String name, int quantity, long unitPrice)
        {
            this.itemId = itemId;
            this.name = name;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.totalPrice = unitPrice * quantity;
        }
    }

    public static final class DropPayload
    {
        public String source;
        public String sourceType;
        public Integer npcId;
        public Long killCount;
        public long totalValue;
        public List<ItemPayload> items = new ArrayList<>();
        public String screenshotBase64;
    }

    public static final class PetPayload
    {
        public String petName;
        public String source;
        public String gameMessage;
    }

    public static final class CollectionLogPayload
    {
        public String itemName;
        public String source;
    }

    public static final class Announcement
    {
        public long id;
        public String title;
        public String message;
        public String kind;
        public String author;
        public boolean showOnLogin;
        public String createdAt;
    }

    public static final class AnnouncementResponse
    {
        public List<Announcement> announcements = new ArrayList<>();
    }

    public static final class PublishAnnouncementRequest
    {
        public String title;
        public String message;
        public String kind;
        public boolean showOnLogin;
    }

    public static final class DropView
    {
        public long id;
        public String playerName;
        public String source;
        public long totalValue;
        public String createdAt;
        public List<ItemPayload> items = new ArrayList<>();
        public String screenshotUrl;
    }

    public static final class DropListResponse
    {
        public List<DropView> drops = new ArrayList<>();
        public int page;
        public int pageSize;
        public int total;
        public int totalPages;
    }

    public static final class MemberSummary
    {
        public String rsn;
        public String role;
        public String accountType;
        public String country;
        public String avatarUrl;
        public long totalXp;
        public double ehp;
        public double ehb;
    }

    public static final class MemberListResponse
    {
        public List<MemberSummary> members = new ArrayList<>();
        public int page;
        public int pageSize;
        public int total;
        public int totalPages;
    }

    public static final class MetricEntry
    {
        public String name;
        public long value;
        public int level;
        public int rank;
        public double ehp;
    }

    public static final class MemberProfile
    {
        public MemberSummary member;
        public String lastUpdated;
        public List<MetricEntry> skills = new ArrayList<>();
        public List<MetricEntry> bosses = new ArrayList<>();
        public List<MetricEntry> activities = new ArrayList<>();
        public List<DropView> recentDrops = new ArrayList<>();
        public List<String> collectionLog = new ArrayList<>();
        public RuneProfileData runeProfile;
    }

    public static final class RuneProfileData
    {
        public String username;
        public String profileUrl;
        public String createdAt;
        public String updatedAt;
        public RuneProfileCollection collectionLog;
        public List<RuneProfileProgress> combatAchievements = new ArrayList<>();
        public List<RuneProfileDiary> achievementDiaries = new ArrayList<>();
    }

    public static final class RuneProfileResponse
    {
        public RuneProfileData profile;
    }

    public static final class RuneProfileCollection
    {
        public int obtained;
        public int total;
        public List<RuneProfileTab> tabs = new ArrayList<>();
    }

    public static final class RuneProfileTab
    {
        public String name;
        public int obtained;
        public int total;
        public List<RuneProfilePage> pages = new ArrayList<>();
    }

    public static final class RuneProfilePage
    {
        public String name;
        public int obtained;
        public int total;
        public List<RuneProfileItem> items = new ArrayList<>();
    }

    public static final class RuneProfileItem
    {
        public int id;
        public String name;
        public int quantity;
    }

    public static final class RuneProfileProgress
    {
        public int id;
        public String name;
        public int completed;
        public int total;
    }

    public static final class RuneProfileDiary
    {
        public int areaId;
        public String area;
        public List<RuneProfileDiaryTier> tiers = new ArrayList<>();
    }

    public static final class RuneProfileDiaryTier
    {
        public String tier;
        public int completed;
        public int total;
    }

    public static final class RankingEntry
    {
        public int rank;
        public String rsn;
        public double value;
    }

    public static final class RankingResponse
    {
        public String metric;
        public String period;
        public String startsAt;
        public String endsAt;
        public List<RankingEntry> entries = new ArrayList<>();
    }

    public static final class StatusResponse
    {
        public boolean ok;
        public String message;
        public GoalAchievement achievement;
    }

    public static final class ItemGoal
    {
        public long id;
        public String playerName;
        public int itemId;
        public String itemName;
        public String startedAt;
    }

    public static final class ItemGoalRequest
    {
        public int itemId;
        public String itemName;

        public ItemGoalRequest(int itemId, String itemName)
        {
            this.itemId = itemId;
            this.itemName = itemName;
        }
    }

    public static final class ItemGoalResponse
    {
        public ItemGoal goal;
    }

    public static final class GoalAchievement
    {
        public int itemId;
        public String itemName;
        public String startedAt;
        public String achievedAt;
        public int elapsedDays;
        public String message;
    }
}

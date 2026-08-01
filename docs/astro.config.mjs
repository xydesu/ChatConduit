import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

export default defineConfig({
  site: 'https://xydesu.github.io',
  base: '/ChatConduit',
  integrations: [
    starlight({
      title: 'ChatConduit',
      description: 'Paper/Purpur 跨服聊天與社交外掛官方維基',
      logo: {
        src: './src/assets/logo.svg',
        alt: 'ChatConduit Logo',
      },
      social: {
        github: 'https://github.com/xydesu/ChatConduit',
      },
      sidebar: [
        {
          label: '快速入門 (Getting Started)',
          items: [
            { label: '專案概述', link: '/getting-started/overview/' },
            { label: '快速安裝指南', link: '/getting-started/installation/' },
          ],
        },
        {
          label: '管理員指南 (For Admin)',
          items: [
            { label: '系統頻道與前綴配置', link: '/admin/channels/' },
            { label: 'Chest GUI Symbol Map 佈局', link: '/admin/gui-layout/' },
            { label: '權限節點與 LuckPerms 授權', link: '/admin/permissions/' },
            { label: '資料庫與 Redis 跨服同步', link: '/admin/database-redis/' },
            { label: 'DiscordSRV 與 CMI AFK 整合', link: '/admin/integrations/' },
          ],
        },
        {
          label: '設定檔完整參考 (Config Reference)',
          items: [
            { label: 'config.yml 完整參數', link: '/admin/config-reference/' },
            { label: 'GUI 佈局設定參考', link: '/admin/gui-config-reference/' },
          ],
        },
        {
          label: '玩家指南 (For Player)',
          items: [
            { label: '零指令頻道發言與切換', link: '/player/channels/' },
            { label: '自建玩家群組頻道', link: '/player/custom-channels/' },
            { label: '跨服私訊與快速回覆', link: '/player/private-messages/' },
            { label: '好友系統與 GUI 社交選單', link: '/player/friends/' },
            { label: '黑名單與隱私保護', link: '/player/blacklist/' },
          ],
        },
        {
          label: '開發者 API (Developer)',
          items: [
            { label: 'PlaceholderAPI 變數大全', link: '/developer/placeholders/' },
            { label: 'Redis Pub/Sub 封包協定規格', link: '/developer/redis-packets/' },
          ],
        },
      ],
    }),
  ],
});

import challengeEmblemUrl from '../../app/src/main/res/drawable/logo_challenge_emblem.png';
import churchMeetingLogoUrl from '../../app/src/main/res/drawable/logo_church_meeting.png';
import nativityHeroUrl from '../../app/src/main/res/drawable/img_nativity_hero.jpg';

export const APP_ASSETS = {
  challengeLogo: challengeEmblemUrl,
  challengeLogoFallback: '/assets/aistudio/image_c3564951.png',
  churchLogo: churchMeetingLogoUrl,
  churchLogoFallback: '/assets/aistudio/image_1a841d56.png',
  nativityHero: nativityHeroUrl,
} as const;

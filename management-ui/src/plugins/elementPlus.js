import { ElLoadingDirective } from "element-plus";

export function installElementPlusFoundation(app) {
  app.directive("loading", ElLoadingDirective);
}

var $scope, ctrl;

//inject dependencies first
beforeEach(inject(function($rootScope) { $scope = $rootScope.$new(); }));
var loginId = "hackerwins";
var projects = [{
  "id": 1,
  "name": "test",
  "owner": "hackerwins",
  "isPublic": true,
  "createdDate": 1371113882933
}, {
  "id": 2,
  "name": "test2",
  "overview": "test hive project",
  "owner": "hackerwins",
  "isPublic": true,
  "createdDate": 1371285617536
}];

describe('MyProjectListCtrl', function() {
  it('Should initialize projects to Loading', inject(function($controller) {
    ctrl = $controller('MyProjectListCtrl', { $scope: $scope });
    expect($scope.projects.length).toEqual(2);
  }));
});

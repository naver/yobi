/**
 * MyProjectListCtrl
 */
var MyProjectListCtrl = function($scope, $filter) {
  $scope.projects = projects;
  $scope.createdProjects = $filter('filter')($scope.projects, {owner: loginId});
  $scope.belongedProjects = _.difference($scope.projects, $scope.createdProjects);
  $scope.predicate = "";
  $scope.reverse = false;

  /**
   * orderByDate
   */
  $scope.orderByDate = function() {
    $scope.predicate = "createdDate";
    $scope.reverse = !$scope.reverse;
  };  

  /**
   * orderByName
   */
  $scope.orderByName = function() {
    $scope.predicate = "name";
    $scope.reverse = !$scope.reverse;
  };
};
